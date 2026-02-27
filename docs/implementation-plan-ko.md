# Clutch-Land 구현 설계안 (Paper 1.21.11 / Java 21 / Gradle / SQLite)

아래 설계는 질문에서 요구한 구현 순서(A~H) 기준으로 작성되며,
핵심 원칙은 **이벤트 핸들러에서 DB 동기 조회 금지 / 시작 시 전체 메모리 로딩 / 비동기 쓰기 큐**입니다.

## A) 전체 패키지/레이어 설계

```text
com.clutchland
├─ ClutchLandPlugin                      # JavaPlugin 엔트리
├─ bootstrap
│  ├─ DependencyRegistry                 # 서비스/레포지토리/리스너 구성
│  └─ LifecycleCoordinator               # onEnable/onDisable 순서 제어
├─ domain
│  ├─ land
│  │  ├─ Land                            # 불변 도메인 모델
│  │  ├─ LandBounds                      # worldUuid + minX/maxX/minZ/maxZ
│  │  ├─ LandOwner                       # UUID owner (nullable)
│  │  ├─ LandMemberRole                  # 확장용 enum (MEMBER, TRUSTED ...)
│  │  └─ LandChunkKey                    # worldUuid + chunkX + chunkZ
│  └─ selection
│     └─ Selection                       # 플레이어 1/2번 좌표 임시 선택
├─ application
│  ├─ command
│  │  ├─ AdminLandCreateCommand          # /토지 생성
│  │  ├─ LandDeleteCommand               # /땅 삭제 (언클레임)
│  │  ├─ LandTransferCommand             # /땅 양도
│  │  └─ ...
│  ├─ service
│  │  ├─ LandQueryService                # 캐시 조회 전용
│  │  ├─ LandPermissionService           # 권한 판단(건축/상호작용)
│  │  ├─ LandManagementService           # 생성/삭제/양도 트랜잭션 정책
│  │  ├─ SelectionService                # 포지션 선택 관리
│  │  └─ MoveDetectionService            # 입장/퇴장 감지
│  └─ dto
│     └─ ...
├─ infrastructure
│  ├─ persistence
│  │  ├─ sqlite
│  │  │  ├─ SqliteDataSourceFactory
│  │  │  ├─ migration
│  │  │  │  ├─ V1__init.sql
│  │  │  │  └─ V2__land_chunks_index.sql
│  │  │  ├─ LandRepositorySqlite
│  │  │  ├─ LandMemberRepositorySqlite
│  │  │  └─ DbMigrationRunner
│  │  └─ async
│  │     ├─ DbWriteQueue                 # 단일 소비자 큐
│  │     └─ DbWriteTask                  # UPSERT/DELETE 작업 단위
│  ├─ cache
│  │  ├─ LandCacheStore                  # 전체 land map
│  │  ├─ LandChunkIndexStore             # land_chunks 역인덱스 캐시
│  │  └─ PlayerLandStateStore            # player -> currentLandId 캐시
│  ├─ listener
│  │  ├─ LandProtectionListener          # Break/Place/Interact/Entity
│  │  └─ PlayerMoveListener              # Move 기반 진입 감지
│  └─ scheduler
│     └─ AsyncExecutorProvider           # BukkitScheduler 추상화
└─ presentation
   ├─ message
   │  ├─ MessageFormatter                # 채팅 메시지
   │  ├─ TitleFormatter                  # 타이틀
   │  └─ MessageKey                      # i18n 키
   └─ command
      └─ argument parser / sender guard
```

### 레이어 원칙
- `domain`: Bukkit API 의존 금지.
- `application`: 정책/유즈케이스 중심, 인프라 인터페이스에 의존.
- `infrastructure`: SQLite/Bukkit 세부 구현.
- `presentation`: 메시지/타이틀 포맷 전담 (로직 분리).

---

## B) SQLite 스키마 및 마이그레이션

> `world`는 **UUID 문자열(TEXT)** 기준 저장.

### 테이블

```sql
CREATE TABLE IF NOT EXISTS lands (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  world_uuid    TEXT NOT NULL,
  min_x         INTEGER NOT NULL,
  max_x         INTEGER NOT NULL,
  min_z         INTEGER NOT NULL,
  max_z         INTEGER NOT NULL,
  owner_uuid    TEXT NULL,
  price         INTEGER NOT NULL DEFAULT 0,
  tier          INTEGER NOT NULL DEFAULT 1,
  tax_rate      REAL NOT NULL DEFAULT 0,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL,
  version       INTEGER NOT NULL DEFAULT 0,
  UNIQUE(world_uuid, min_x, max_x, min_z, max_z)
);

CREATE TABLE IF NOT EXISTS land_members (
  land_id       INTEGER NOT NULL,
  member_uuid   TEXT NOT NULL,
  role          TEXT NOT NULL DEFAULT 'MEMBER',
  created_at    INTEGER NOT NULL,
  PRIMARY KEY(land_id, member_uuid),
  FOREIGN KEY(land_id) REFERENCES lands(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS land_chunks (
  land_id       INTEGER NOT NULL,
  world_uuid    TEXT NOT NULL,
  chunk_x       INTEGER NOT NULL,
  chunk_z       INTEGER NOT NULL,
  PRIMARY KEY(land_id, world_uuid, chunk_x, chunk_z),
  FOREIGN KEY(land_id) REFERENCES lands(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lands_world_owner
  ON lands(world_uuid, owner_uuid);

CREATE INDEX IF NOT EXISTS idx_land_chunks_lookup
  ON land_chunks(world_uuid, chunk_x, chunk_z);
```

### 마이그레이션 전략
- `schema_version` 메타 테이블로 버전 관리.
- 시작 시 `DbMigrationRunner`가 순차 적용.
- 실패 시 플러그인 disable (데이터 불일치 방지).
- 확장 포인트:
  - 세금: `tax_rate` 이미 포함.
  - 등급: `tier` 포함.
  - 토지 제한: 추후 `player_land_limits` 테이블 추가 가능.

---

## C) Selection 구현 + 겹침 방지 로직

### Selection
- `/토지 생성`은 관리자만 사용.
- `SelectionService`가 `Map<UUID, Selection>` 보관.
- Selection: `worldUuid`, `pos1`, `pos2`.
- `toBounds()`에서 min/max 정규화.
- world 다르면 거부.

### 겹침 방지 (2D X-Z)
- 신규 `bounds` 생성 시, 같은 world의 기존 land와 충돌 검사.
- 충돌식:
  - `!(new.maxX < old.minX || new.minX > old.maxX || new.maxZ < old.minZ || new.minZ > old.maxZ)`
- 단, 성능상 전체 순회 대신 **land_chunks 후보군만 검사**:
  1. 신규 bounds가 걸치는 chunk 집합 계산
  2. chunk 역인덱스로 후보 land id 집합 조회
  3. 후보에 대해 정밀 2D AABB 검사

정책 반영:
- 등록 토지만 구매 가능.
- owner가 NULL이면 빈 땅(보호됨, OP 제외 건축 불가).

---

## D) `land_chunks` 기반 청크 역인덱스 설계

### 목적
- 위치 조회/겹침 검사를 O(해당 청크 토지 수)로 축소.

### 메모리 구조
- `Map<LandChunkKey, IntSet landIds>`
- `Map<Integer landId, Land>`

### 생성/갱신
- 서버 시작 로딩 시 DB `land_chunks` 전량 로드.
- 토지 생성/경계변경/삭제(레코드 유지 정책이면 경계변경 없음) 시:
  - 메모리 인덱스 선반영
  - DB writer 큐에 upsert/delete enqueue

### 일관성
- 쓰기 실패 시 재시도 + 경고 로그.
- 심각 장애 시 주기적 snapshot 재동기화 명령 제공(관리자 명령).

---

## E) LandQueryService + 캐시 구조

### 캐시
1. `landById: Int2ObjectMap<Land>`
2. `membersByLandId: Int2ObjectMap<Set<UUID>>`
3. `chunkToLandIds: Map<LandChunkKey, IntSet>`
4. `playerCurrentLandId: Map<UUID, Integer?>`

### 주요 API
- `Optional<Land> findLandAt(LocationLike loc)`
  - loc -> chunk key
  - 후보 landIds 조회
  - bounds contains(X,Z) 정밀 검사
- `boolean isRegisteredLand(LocationLike loc)`
- `boolean isOwnedLand(LocationLike loc)`
- `Set<UUID> getMembers(int landId)`

### null/예외 안전
- `world == null` 혹은 UUID 변환 실패 시 empty 반환.
- 캐시 miss는 즉시 deny 기본값을 반환(보안 우선).

---

## F) LandPermissionService + 이벤트 리스너

### 권한 규칙 (요약)
1. 해당 위치 land 없음 → 기본 deny (등록 외 지역 구매 불가 정책과 별개로, 건축 정책은 서버 정책에 맞게 선택. 질문 정책상 등록 외 지역도 사실상 보호하려면 deny 권장)
2. land 있음 + owner NULL → OP만 허용
3. owner == player → 허용
4. members 포함 → 허용
5. 그 외 deny

### 서비스 메서드
- `canBreak(player, blockLoc)`
- `canPlace(player, blockLoc)`
- `canInteract(player, clickedLoc)`
- `canEntityAffect(playerOrSource, targetLoc)`

### 리스너
- `BlockBreakEvent`
- `BlockPlaceEvent`
- `PlayerInteractEvent`
- `EntityExplodeEvent`, `EntityChangeBlockEvent`, `HangingBreakByEntityEvent` 등

모든 리스너는:
- 캐시 조회만 수행
- DB 접근 금지
- 거부 시 `MessageFormatter`/`TitleFormatter` 호출

---

## G) PlayerMove 기반 입장 감지 + landId 캐시

### 최적화
- `PlayerMoveEvent`에서 블록 좌표/월드 동일하면 즉시 return.
- `fromLandId`/`toLandId`를 캐시와 query로 비교.

### 흐름
1. `to` 위치 landId 계산
2. `playerCurrentLandId` 이전 값과 비교
3. 변경 시:
   - exit 메시지 (이전 land 존재)
   - enter 메시지 (신규 land 존재)
   - 캐시 업데이트

### 안정성
- 텔레포트/리스폰/월드변경 이벤트에서도 동일 로직 재사용.
- null location 방어.

---

## H) 비동기 DB writer

### 구조
- `LinkedBlockingQueue<DbWriteTask>`
- 단일 워커 스레드(순서 보장)
- 작업 유형:
  - LAND_UPSERT
  - LAND_OWNER_UPDATE (언클레임/양도)
  - LAND_MEMBERS_REPLACE (삭제/양도 시 전원 삭제 반영)
  - LAND_CHUNKS_REPLACE

### 정책 반영 포인트
- `/땅 삭제`:
  - owner_uuid = NULL
  - members 전량 삭제
  - lands 레코드 유지
- `/땅 양도`:
  - owner_uuid = newOwner
  - members 전량 삭제

### 신뢰성
- graceful shutdown 시 큐 drain + flush timeout.
- 실패 task 재시도(backoff).
- 반복 실패는 dead-letter 로그로 관리자 알림.

---

## 추가 제안 (확장성)

- `PolicyConfig` 분리:
  - 등록 외 지역 건축 허용 여부
  - owner NULL 토지 상호작용 허용 범위
- `PermissionNode` 체계화:
  - `clutchland.admin.create`, `clutchland.bypass`
- 추후 ShopMoney 연동 대비:
  - `EconomyGateway` 인터페이스만 먼저 정의
- Scoreboard/MoveCommands 연동 대비:
  - `LandEnterEvent` 커스텀 이벤트 발행

