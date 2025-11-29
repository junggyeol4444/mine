# MinecraftJobPlugin

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10-green.svg)](https://www.minecraft.net/)
[![Spigot API](https://img.shields.io/badge/Spigot%20API-1.21.10-orange.svg)](https://www.spigotmc.org/)
[![License](https://img.shields.io/badge/license-Custom-blue.svg)]()

마인크래프트 서버를 위한 종합 직업 시스템 플러그인입니다. 플레이어가 다양한 직업을 선택하고, 스킬을 습득하며, 퀘스트를 완료하고, 전용 상점을 이용할 수 있는 RPG 스타일 게임플레이를 제공합니다.

## 📋 목차

- [주요 기능](#-주요 기능)
- [시스템 구조](#-시스템 구조)
- [설치 방법](#-설치 방법)
- [명령어](#-명령어)
- [설정](#-설정)
- [직업 시스템](#-직업 시스템)
- [스킬 시스템](#-스킬 시스템)
- [퀘스트 시스템](#-퀘스트 시스템)
- [등급 시스템](#-등급 시스템)
- [경제 시스템](#-경제 시스템)
- [개발자 정보](#-개발자 정보)

## 🎮 주요 기능

### 1. **직업 시스템 (Job System)**
- 8개의 독특한 직업 (탐험가, 목수, 광부, 사냥꾼, 농부, 어부, 대장장이, 연금술사, 요리사)
- 다중 직업 보유 가능 (설정 가능)
- 직업별 시작 아이템 지급
- 직업 조합(콤보) 시스템으로 시너지 효과
- 희귀도 등급별 직업 분류

### 2. **스킬 시스템 (Skill System)**
- 18개 이상의 직업별 고유 스킬
- 레벨업 시스템 (최대 10레벨)
- 경험치 획득 및 스킬 성장
- 스킬북을 통한 스킬 발동
- 쿨타임 및 레벨별 효과 증가

### 3. **퀘스트 시스템 (Quest System)**
- 직업별 일일/주간 퀘스트
- 자동 진행도 추적
- 다양한 퀘스트 타입 (블록 채집, 몹 처치, 작물 수확, 낚시)
- 완료 시 금전 및 특성 포인트 보상

### 4. **등급 시스템 (Grade System)**
- 5단계 등급 (D → C → B → A → S)
- 활동 횟수 및 퀘스트 완료로 승급
- 등급별 상점 할인 및 스킬 경험치 보너스
- 등급별 전용 보상 및 칭호

### 5. **경제 시스템 (Economy System)**
- Vault 연동 지원 (선택사항)
- 내부 경제 시스템
- 직업별 전용 상점
- 구매/판매 시스템
- 등급별 할인 적용

### 6. **장비 강화 시스템**
- 아이템 업그레이드 (최대 20단계)
- 랜덤 인챈트 강화
- 비용 증가 시스템
- 내구성 제외 인챈트 시스템

### 7. **특성 시스템 (Trait System)**
- 직업별 고유 특성
- 퀘스트 완료 시 포인트 획득
- 특성 선택을 통한 캐릭터 커스터마이징

### 8. **데이터 관리**
- YAML 기반 데이터 저장
- 비동기 저장으로 서버 성능 최적화
- 자동 저장 시스템
- 플레이어별 개별 데이터 관리

## 🏗 시스템 구조

```
MinecraftJobPlugin/
├── src/main/java/org/blog/minecraftJobPlugin/
│   ├── JobPlugin.java                    # 메인 플러그인 클래스
│   ├── command/                          # 명령어 핸들러
│   │   ├── ComboCommand.java
│   │   ├── GradeCommand.java
│   │   ├── JobCommand.java
│   │   ├── QuestCommand.java
│   │   ├── ShopCommand.java
│   │   ├── SkillCommand.java
│   │   └── UpgradeCommand.java
│   ├── econ/                            # 경제 시스템
│   │   └── EconomyManager.java
│   ├── equipment/                       # 장비 시스템
│   │   └── EquipmentManager.java
│   ├── gui/                            # GUI 인터페이스
│   │   ├── JobSelectionGUI.java
│   │   ├── QuestGUI.java
│   │   ├── ShopGUI.java
│   │   └── SkillTreeGUI.java
│   ├── job/                            # 직업 관련 클래스
│   │   ├── Job.java
│   │   ├── JobMeta.java
│   │   ├── JobComboManager.java
│   │   └── JobGradeManager.java
│   ├── listener/                       # 이벤트 리스너
│   │   ├── ComboCheckListener.java
│   │   ├── JobEventListener.java
│   │   ├── JobGuiListener.java
│   │   ├── PlayerDataListener.java
│   │   ├── QuestTrackerListener.java
│   │   ├── SkillBuffListener.java
│   │   └── SkillItemListener.java
│   ├── manager/                        # 핵심 관리자
│   │   └── JobManager.java
│   ├── quest/                         # 퀘스트 시스템
│   │   ├── QuestManager.java
│   │   ├── QuestMeta.java
│   │   └── QuestProgress.java
│   ├── skill/                         # 스킬 시스템
│   │   ├── SkillManager.java
│   │   ├── SkillMeta.java
│   │   ├── TraitManager.java
│   │   └── TraitMeta.java
│   └── util/                          # 유틸리티
│       ├── ActionBarUtil.java
│       ├── ConfigUtil.java
│       ├── LocalStorage.java
│       └── PluginDataUtil.java
└── src/main/resources/
    ├── plugin.yml                     # 플러그인 메타데이터
    ├── config.yml                     # 메인 설정
    └── config/                        # 상세 설정 파일
        ├── jobs.yml
        ├── skills.yml
        ├── quests.yml
        ├── shops.yml
        ├── grades.yml
        ├── combos.yml
        └── traits.yml
```

## 📦 설치 방법

### 필수 요구사항
- **Minecraft 서버**: Paper/Spigot 1.21.10 이상
- **Java**: Java 17 이상
- **선택사항**: Vault 플러그인 (외부 경제 연동)

### 설치 단계

1. **플러그인 다운로드**
   ```bash
   # 릴리즈 페이지에서 최신 .jar 파일 다운로드
   ```

2. **플러그인 설치**
   ```bash
   # 서버의 plugins 폴더에 .jar 파일 복사
   cp MinecraftJobPlugin-1.0.0.jar /서버경로/plugins/
   ```

3. **서버 시작**
   ```bash
   # 서버 시작 (설정 파일 자동 생성)
   ./start.sh
   ```

4. **설정 파일 편집** (선택사항)
   ```bash
   # /plugins/MinecraftJobPlugin/config.yml 및 하위 설정 파일 수정
   ```

5. **서버 재시작 또는 리로드**
   ```bash
   /reload confirm
   # 또는
   /job reload  # 관리자 권한 필요
   ```

## 🎯 명령어

### 플레이어 명령어

#### `/job` - 직업 관리
```
/job              # 직업 선택 GUI 열기
/job list         # 모든 직업 목록 보기
/job info <직업>   # 특정 직업 정보 확인
/job my          # 내 직업 정보 보기
```

#### `/skill` - 스킬 관리
```
/skill            # 스킬 트리 GUI 열기
/skill list       # 내 스킬 목록 보기
/skill use <스킬>  # 스킬 사용
/skill info <스킬> # 스킬 상세 정보
```

#### `/quest` - 퀘스트 관리
```
/quest              # 퀘스트 GUI 열기
/quest list         # 퀘스트 목록 보기
/quest submit <ID>  # 퀘스트 제출
/quest progress     # 진행 중인 퀘스트 확인
```

#### `/shop` - 상점
```
/shop             # 현재 직업 상점 열기
```

#### `/upgrade` - 장비 강화
```
/upgrade          # 손에 든 아이템 업그레이드
```

#### `/combo` - 직업 조합
```
/combo            # 내 조합 목록
/combo list       # 모든 조합 보기
/combo check      # 해금 가능 확인
```

#### `/grade` - 등급 관리
```
/grade            # 현재 등급 보기
/grade list       # 모든 직업 등급
/grade info <등급> # 등급 상세 정보
/grade check      # 승급 확인
```

### 관리자 명령어

```
/job reload       # 플러그인 리로드
/job debug        # 디버그 정보 출력
```

## ⚙ 설정

### config.yml - 메인 설정

```yaml
# 경제 시스템
economy:
  default_reward: 50           # 기본 행동 보상
  currency_symbol: "원"        # 통화 기호
  starting_balance: 0          # 시작 금액
  autosave_seconds: 300        # 자동 저장 주기 (초)

# 장비 업그레이드
equipment:
  max_upgrade_level: 20        # 최대 업그레이드 레벨
  upgrade_base_price: 100      # 기본 가격
  upgrade_multiplier: 1.8      # 가격 배율
  max_enchant_level_per_type: 10  # 인챈트별 최대 레벨

# 직업 시스템
job:
  max_jobs_per_player: -1      # 최대 직업 수 (-1 = 무제한)
  first_job_cost: 0            # 첫 직업 비용
  second_job_cost: 50000       # 두 번째 직업 비용
  third_job_cost: 100000       # 세 번째 직업 비용

# Vault 연동
vault:
  use_vault_economy: true      # Vault 사용 여부
  warn_if_not_found: false     # 미설치 시 경고

# 성능 최적화
performance:
  async_save: true             # 비동기 저장
  use_cache: true              # 캐시 사용
```

### jobs.yml - 직업 정의

```yaml
jobs:
  explorer:
    display: "탐험가"
    description: "지도 제작 및 구조물 탐색에 특화된 직업"
    skills:
      - geoScan
      - warpPoint
    rarity: common
    startingItems:
      - material: COMPASS
        amount: 1
        name: "§6탐험가의 나침반"
```

### skills.yml - 스킬 정의

```yaml
skills:
  geoScan:
    display: "지형 스캔"
    cooldown: 300
    levels:
      1:
        effect: "근처 지형 하이라이트"
      2:
        effect: "반경 증가"
```

## 👔 직업 시스템

### 사용 가능한 직업

| 직업 | 희귀도 | 특징 | 주요 스킬 |
|------|--------|------|-----------|
| 탐험가 | Common | 지도 제작, 구조물 탐색 | 지형 스캔, 워프 지점 |
| 목수 | Common | 나무 채집, 목재 가공 | 빠른 벌목, 효율적 조각 |
| 광부 | Common | 광물 채굴, 광맥 탐사 | 광맥 감지, 폭파 채굴 |
| 사냥꾼 | Common | 몹 사냥, 추적 | 치명타 사격, 추적 |
| 농부 | Common | 작물 재배, 수확 | 풍작, 영양 강화 |
| 어부 | Uncommon | 낚시, 희귀 아이템 획득 | 희귀 입질, 심해 |
| 대장장이 | Uncommon | 무기/방어구 제작, 강화 | 정밀 단조, 열처리 |
| 연금술사 | Rare | 포션 제작, 추출물 제작 | 이중 증류, 엘릭서 제작 |
| 요리사 | Uncommon | 음식 조리, 버프 생성 | 명장 요리, 양념 마스터 |

### 직업 조합 (콤보)

특정 직업 조합 시 특별한 보너스를 획득합니다:

- **광부 + 대장장이**: 특수 무기 제작 레시피 해금
- **농부 + 요리사**: 고급 요리 레시피 해금
- **탐험가 + 어부**: 해상 워프 포인트 추가

## 🔮 스킬 시스템

### 스킬 종류

#### 탐험가 스킬
- **지형 스캔 (geoScan)**: 주변 광물 하이라이트
- **워프 지점 (warpPoint)**: 순간이동 지점 설치

#### 목수 스킬
- **빠른 벌목 (fastLumber)**: 채굴 속도 버프
- **효율적 조각 (efficientCarve)**: 목재 드롭 증가

#### 광부 스킬
- **광맥 감지 (veinDetect)**: 주변 광맥 위치 파악
- **폭파 채굴 (blastMining)**: 주변 블록 한번에 파괴

#### 사냥꾼 스킬
- **치명타 사격 (criticalShot)**: 화살 데미지 증가
- **추적 (tracking)**: 주변 몹 감지

#### 농부 스킬
- **풍작 (bumperCrop)**: 수확량 증가
- **영양 강화 (nutrientBoost)**: 작물 성장 촉진

#### 어부 스킬
- **희귀 입질 (rareCatch)**: 희귀 물고기 확률 증가
- **심해 (deepSea)**: 수중 호흡 + 야간 투시

#### 대장장이 스킬
- **정밀 단조 (precisionForge)**: 제작 품질 증가
- **열처리 (tempering)**: 장비 내구도 회복

#### 연금술사 스킬
- **이중 증류 (doubleBrew)**: 포션 효과 증가
- **엘릭서 제작 (elixirCraft)**: 특수 효과 생성

#### 요리사 스킬
- **명장 요리 (gourmetCook)**: 음식 버프 강화
- **양념 마스터 (spiceMaster)**: 포만감 회복

### 스킬 레벨업

- 스킬 사용 시 경험치 획득
- 레벨업 시 효과 증가 및 쿨타임 감소
- 최대 레벨: 10
- 등급 보너스로 경험치 획득량 증가

## 📜 퀘스트 시스템

### 퀘스트 타입

1. **collect_block**: 특정 블록 채집
   - 예: 철광석 30개 채굴
   
2. **kill_mob**: 몹 처치
   - 예: 몹 20마리 처치

3. **harvest**: 작물 수확
   - 예: 밀 200개 수확

4. **catch_rare**: 희귀 물고기 낚시
   - 예: 희귀 물고기 5마리

### 퀘스트 보상

- 금전 보상
- 특성 포인트 1개
- 등급 상승 진행도 증가

## 🏆 등급 시스템

### 등급 구조

| 등급 | 이름 | 승급 조건 | 보상 |
|------|------|-----------|------|
| D | 초보자 | 직업 획득 | 기본 스킬 |
| C | 수습 | 활동 200회 | 스킬 경험치 +10% |
| B | 숙련 | 활동 500회 또는 퀘스트 10회 | 특성 포인트 +2, 상점 할인 5% |
| A | 전문가 | 활동 1000회 + 퀘스트 20회 | 특성 포인트 +3, 상점 할인 10% |
| S | 마스터 | 활동 2000회 + 퀘스트 50회 + 모든 스킬 Lv5 | 특성 포인트 +5, 상점 할인 20%, 칭호 |

### 등급 혜택

- **상점 할인**: 등급이 높을수록 할인율 증가
- **스킬 경험치 보너스**: 등급별 10%~50% 보너스
- **특성 포인트**: 승급 시 포인트 지급
- **전용 칭호**: S등급 달성 시 칭호 획득

## 💰 경제 시스템

### Vault 연동

플러그인은 Vault를 선택적으로 지원합니다:

- **Vault 있음**: 외부 경제 플러그인 (EssentialsX 등) 사용
- **Vault 없음**: 내부 경제 시스템 자동 사용

### 상점 시스템

- **직업별 전용 상점**: 각 직업마다 고유 상점
- **구매/판매**: 아이템 구매 및 판매 모두 지원
- **등급 할인**: 높은 등급일수록 할인 적용
- **대량 판매**: 여러 아이템 동시 판매 가능

### 수입원

1. **퀘스트 완료**: 100~300원
2. **아이템 판매**: 상점에서 아이템 판매
3. **등급 승급 보상**: 등급 상승 시 보너스
4. **스킬 활용**: 특정 스킬로 추가 수입

## 🛠 개발자 정보

### 기술 스택

- **언어**: Java 17+
- **빌드 도구**: Gradle (추정)
- **API**: Spigot/Paper 1.21.10
- **의존성**: Vault (선택사항)

### 아키텍처 특징

1. **모듈화된 구조**: 기능별 패키지 분리
2. **이벤트 기반**: Bukkit 이벤트 시스템 활용
3. **비동기 처리**: 데이터 저장 시 서버 성능 최적화
4. **캐싱**: 메모리 캐시로 디스크 I/O 최소화
5. **YAML 기반**: 설정 및 데이터 저장

### 데이터 구조

```
plugins/MinecraftJobPlugin/
├── config.yml                    # 메인 설정
├── config/                       # 세부 설정
│   ├── jobs.yml
│   ├── skills.yml
│   ├── quests.yml
│   ├── shops.yml
│   ├── grades.yml
│   ├── combos.yml
│   └── traits.yml
└── data/
    └── player/                   # 플레이어별 데이터
        └── <UUID>.yml
```

### 확장 가능성

플러그인은 다음과 같은 확장이 가능합니다:

1. **새로운 직업 추가**: `jobs.yml`에 직업 정의
2. **커스텀 스킬**: `skills.yml` + `SkillManager` 확장
3. **퀘스트 추가**: `quests.yml`에 퀘스트 정의
4. **상점 아이템**: `shops.yml`에 아이템 추가
5. **조합 추가**: `combos.yml`에 조합 정의

### 성능 최적화

- ✅ 비동기 데이터 저장
- ✅ 메모리 캐싱 (ConcurrentHashMap 사용)
- ✅ 자동 저장 주기 설정 가능
- ✅ 이벤트 리스너 최적화
- ✅ 리플렉션 최소화 (Vault 연동 시만 사용)

## 📝 라이선스

이 프로젝트의 라이선스는 명시되어 있지 않습니다. 사용 전 저작권자에게 문의하세요.

## 👨‍💻 제작자

**junggyeol4444**

## 🤝 기여

기여 방법에 대한 정보가 필요하신 경우 저장소 관리자에게 문의하세요.

## 📞 지원

버그 리포트 또는 기능 제안은 이슈 트래커를 통해 제출해주세요.

---

**Note**: 이 README는 제공된 소스 코드를 기반으로 작성되었습니다. 실제 사용 시 세부 사항이 다를 수 있습니다.
