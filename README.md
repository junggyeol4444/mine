# 📦 MinecraftJobPlugin - 직업 중복 방지 및 비용 시스템 적용 가이드

## 🎯 변경 사항 요약

1. **직업 중복 획득 방지**
   - 한 번 획득한 직업은 다시 획득 불가
   - "이미 보유한 직업입니다!" 메시지 출력

2. **직업 획득 비용 시스템**
   - 첫 직업: 무료 (0원)
   - 두 번째 직업: 50,000원
   - 세 번째 직업: 100,000원
   - 네 번째 이후: 200,000원

3. **자동 데이터 로드/저장**
   - 플레이어 접속 시 자동 로드
   - 플레이어 퇴장 시 자동 저장 (비동기)
   - 5분마다 자동 저장

---

## 📂 수정/추가된 파일 목록

### 1️⃣ **필수 수정 파일**

```
src/main/resources/
├── config.yml                    ⭐ 새 버전으로 교체
└── plugin.yml                    (softdepend: [Vault] 추가)

src/main/java/org/blog/minecraftJobPlugin/
├── JobPlugin.java                ⭐ 완전 교체
├── commands/
│   └── JobCommand.java           ⭐ 완전 교체
├── listeners/
│   ├── JobGuiListener.java       ⭐ 완전 교체
│   └── PlayerDataListener.java   ⭐ 새로 추가
└── manager/
    └── JobManager.java            ⭐ 완전 교체
```

---

## 📋 상세 파일별 변경 내용

### **1. config.yml**
**위치:** `src/main/resources/config.yml`

**추가된 설정:**
```yaml
job:
  can_abandon_job: false          # 직업 포기 불가
  first_job_cost: 0               # 첫 직업 무료
  second_job_cost: 50000          # 두 번째 직업 비용
  third_job_cost: 100000          # 세 번째 직업 비용
  additional_job_cost: 200000     # 네 번째 이후 비용

messages:
  job_already_owned: "§c이미 보유한 직업입니다!"
  job_acquired_paid: "§a직업을 획득했습니다: §b%job% §7(비용: §e%cost%%currency%§7)"
  job_insufficient_funds: "§c직업을 획득하기 위한 돈이 부족합니다!"
  job_max_reached: "§c최대 직업 개수에 도달했습니다!"
```

---

### **2. JobManager.java**
**위치:** `src/main/java/org/blog/minecraftJobPlugin/manager/JobManager.java`

**추가된 필드:**
```java
// 플레이어 데이터 (UUID -> 보유 직업 목록)
private final Map<UUID, List<String>> playerJobs = new ConcurrentHashMap<>();

// 플레이어 활성 직업 (UUID -> 장착 중인 직업)
private final Map<UUID, String> activeJobs = new ConcurrentHashMap<>();
```

**추가된 메서드:**
- `hasJob(Player, String)` - 직업 보유 여부 확인
- `getPlayerJobs(Player)` - 보유 직업 목록 반환
- `addJob(Player, String)` - 직업 추가 (중복 방지)
- `removeJob(Player, String)` - 직업 제거
- `setActiveJob(Player, String)` - 활성 직업 설정
- `getActiveJob(Player)` - 활성 직업 반환
- `savePlayerData(Player)` - 플레이어 데이터 저장
- `loadPlayerData(Player)` - 플레이어 데이터 로드
- `saveAll()` - 모든 플레이어 데이터 저장
- `canAcquireMoreJobs(Player)` - 추가 직업 획득 가능 여부
- `canAbandonJob()` - 직업 포기 가능 여부

---

### **3. JobGuiListener.java**
**위치:** `src/main/java/org/blog/minecraftJobPlugin/listeners/JobGuiListener.java`

**핵심 변경사항:**

**acquireJob() 메서드:**
```java
private void acquireJob(Player player, Job job) {
    // 1. 중복 확인
    if (jobManager.hasJob(player, jobName)) {
        player.sendMessage("§c이미 보유한 직업입니다!");
        return;
    }
    
    // 2. 최대 직업 개수 확인
    if (maxJobs > 0 && ownedJobs.size() >= maxJobs) {
        player.sendMessage("§c최대 직업 개수에 도달했습니다!");
        return;
    }
    
    // 3. 비용 계산 및 확인
    double cost = calculateJobCost(player, jobName);
    if (balance < cost) {
        player.sendMessage("§c돈이 부족합니다!");
        return;
    }
    
    // 4. 비용 차감
    economyManager.withdraw(player.getUniqueId(), cost);
    
    // 5. 직업 추가
    jobManager.addJob(player, jobName);
}
```

**calculateJobCost() 메서드:**
```java
private double calculateJobCost(Player player, String jobName) {
    // 특정 직업 비용이 설정되어 있는지 확인
    if (plugin.getConfig().contains("job.job_specific_costs." + jobName)) {
        return plugin.getConfig().getDouble("job.job_specific_costs." + jobName);
    }
    
    // 보유 직업 개수에 따른 비용
    int ownedJobCount = jobManager.getPlayerJobs(player).size();
    
    switch (ownedJobCount) {
        case 0: return plugin.getConfig().getDouble("job.first_job_cost", 0);
        case 1: return plugin.getConfig().getDouble("job.second_job_cost", 50000);
        case 2: return plugin.getConfig().getDouble("job.third_job_cost", 100000);
        default: return plugin.getConfig().getDouble("job.additional_job_cost", 200000);
    }
}
```

---

### **4. PlayerDataListener.java** ⭐ 새로 추가
**위치:** `src/main/java/org/blog/minecraftJobPlugin/listeners/PlayerDataListener.java`

**기능:**
- 플레이어 접속 시 모든 데이터 로드
- 플레이어 퇴장 시 모든 데이터 저장 (비동기)
- 직업 조합 자동 체크

```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    plugin.getJobManager().loadPlayerData(player);
    // ... 다른 매니저들도 로드
}

@EventHandler
public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    // 비동기 저장
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
        savePlayerData(player);
    });
}
```

---

### **5. JobPlugin.java**
**위치:** `src/main/java/org/blog/minecraftJobPlugin/JobPlugin.java`

**변경사항:**
- `registerListeners()` 메서드에 `PlayerDataListener` 추가
- Vault 초기화 로직 개선
- 자동 저장 스케줄러 추가

```java
private void registerListeners() {
    // ... 기존 리스너들
    getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);
}

private void startAutoSaveScheduler() {
    int saveIntervalSeconds = getConfig().getInt("economy.autosave_seconds", 300);
    // 5분마다 자동 저장
    Bukkit.getScheduler().runTaskTimer(this, this::saveAllData, 
        saveIntervalTicks, saveIntervalTicks);
}
```

---

### **6. JobCommand.java**
**위치:** `src/main/java/org/blog/minecraftJobPlugin/commands/JobCommand.java`

**추가된 명령어:**
- `/job` - GUI 열기
- `/job list` - 직업 목록 보기
- `/job info <직업>` - 직업 상세 정보
- `/job my` - 내 직업 확인
- `/job reload` - 플러그인 리로드 (관리자)
- `/job debug` - 디버그 정보 출력 (관리자)

---

## 🔧 적용 방법

### **Step 1: 파일 교체**

```bash
# 1. config.yml 교체
cp config.yml src/main/resources/config.yml

# 2. Java 파일 교체
cp JobManager.java src/main/java/org/blog/minecraftJobPlugin/manager/
cp JobPlugin.java src/main/java/org/blog/minecraftJobPlugin/
cp JobGuiListener.java src/main/java/org/blog/minecraftJobPlugin/listeners/
cp JobCommand.java src/main/java/org/blog/minecraftJobPlugin/commands/

# 3. 새 파일 추가
cp PlayerDataListener.java src/main/java/org/blog/minecraftJobPlugin/listeners/
```

### **Step 2: plugin.yml 수정**

`src/main/resources/plugin.yml` 파일에서:

```yaml
# 변경 전
depend: [Vault]

# 변경 후
softdepend: [Vault]
```

### **Step 3: 재빌드**

```bash
mvn clean package
```

### **Step 4: 서버 적용**

```bash
# 기존 플러그인 삭제
rm server/plugins/job-1.0.0.jar

# 새 플러그인 복사
cp target/job-1.0.0.jar server/plugins/

# 서버 재시작
```

---

## 🎮 테스트 시나리오

### **1. 첫 직업 획득 (무료)**

```
1. 플레이어 접속
2. /job 입력
3. "광부" 좌클릭
4. 메시지: "직업을 획득했습니다: 광부"
5. 자동으로 장착됨
6. /job my 로 확인
```

**결과:**
- ✅ 잔액 변화 없음 (무료)
- ✅ 광부 직업 획득
- ✅ 광부 자동 장착

---

### **2. 두 번째 직업 획득 (50,000원)**

```
1. /money 로 잔액 확인 (예: 100,000원)
2. /job 입력
3. "대장장이" 좌클릭
4. 메시지: "직업을 획득했습니다: 대장장이 (비용: 50,000원)"
5. /money 로 잔액 재확인 (예: 50,000원)
```

**결과:**
- ✅ 50,000원 차감
- ✅ 대장장이 직업 획득
- ✅ 광부는 여전히 장착 상태

---

### **3. 직업 중복 획득 시도**

```
1. /job 입력
2. "광부" 다시 좌클릭
3. 메시지: "이미 보유한 직업입니다!"
4. 인벤토리 닫힘
```

**결과:**
- ✅ 중복 획득 방지
- ✅ 돈 차감 없음

---

### **4. 잔액 부족 시**

```
1. 잔액: 10,000원 (세 번째 직업 비용: 100,000원)
2. /job 입력
3. "어부" 좌클릭
4. 메시지: "직업을 획득하기 위한 돈이 부족합니다! (필요: 100,000원)"
```

**결과:**
- ✅ 직업 획득 실패
- ✅ 명확한 부족 금액 안내

---

### **5. 직업 장착 변경**

```
1. /job 입력
2. "대장장이" 우클릭
3. 메시지: "장착 직업 변경: 대장장이"
4. GUI 새로고침 (대장장이에 [장착중] 표시)
```

**결과:**
- ✅ 비용 없이 장착 변경
- ✅ 즉시 반영

---

### **6. 데이터 저장/로드 테스트**

```
1. 플레이어가 직업 2개 획득 (광부, 대장장이)
2. 대장장이 장착
3. 서버 종료
4. 서버 재시작
5. 플레이어 재접속
6. /job my 입력
```

**결과:**
- ✅ 광부, 대장장이 보유 상태 유지
- ✅ 대장장이 장착 상태 유지
- ✅ 데이터 손실 없음

---

## ⚙️ 비용 커스터마이징

### **전체 비용 변경**

`config.yml`:
```yaml
job:
  first_job_cost: 0           # 첫 직업
  second_job_cost: 100000     # 두 번째 (10만원)
  third_job_cost: 500000      # 세 번째 (50만원)
  additional_job_cost: 1000000  # 네 번째 이후 (100만원)
```

### **특정 직업만 비싸게**

`config.yml`:
```yaml
job:
  # 기본 비용은 위와 동일
  
  # 특정 직업 개별 설정 (기본 비용 무시)
  job_specific_costs:
    대장장이: 200000   # 대장장이는 항상 20만원
    연금술사: 300000   # 연금술사는 항상 30만원
    요리사: 150000     # 요리사는 항상 15만원
```

---

## 📊 데이터 저장 위치

```
plugins/MinecraftJobPlugin/
├── config.yml
├── config/
│   ├── jobs.yml
│   ├── quests.yml
│   ├── skills.yml
│   └── ...
└── data/
    └── player/
        ├── UUID1.yml  ← 플레이어별 데이터
        ├── UUID2.yml
        └── UUID3.yml
```

**플레이어 데이터 예시 (UUID.yml):**
```yaml
jobs:
  - 광부
  - 대장장이
active_job: 대장장이
```

---

## 🐛 트러블슈팅

### **문제 1: "이미 보유한 직업입니다!" 안 뜨고 계속 획득됨**

**원인:** JobManager의 `hasJob()` 메서드가 제대로 작동하지 않음

**해결:**
1. JobManager.java가 완전히 교체되었는지 확인
2. `playerJobs` 필드가 제대로 선언되었는지 확인
3. 서버 재시작 후 재테스트

---

### **문제 2: 비용이 차감되지 않음**

**원인:** EconomyManager 연동 문제

**해결:**
1. config.yml에서 `vault.use_vault_economy: true` 확인
2. Vault + EssentialsX 설치 확인
3. 서버 로그에서 "Vault Economy 연동 완료" 메시지 확인

---

### **문제 3: 서버 재시작 후 직업 데이터 사라짐**

**원인:** PlayerDataListener 미등록 또는 저장 실패

**해결:**
1. PlayerDataListener.java 파일이 추가되었는지 확인
2. JobPlugin.java의 `registerListeners()`에 등록되었는지 확인
3. `plugins/MinecraftJobPlugin/data/player/` 폴더에 UUID.yml 파일 생성 확인

---

## 📞 추가 도움

더 궁금한 점이나 오류가 발생하면 알려주세요!

- `/job debug` - 디버그 정보 출력 (관리자)
- 콘솔 로그 확인
- UUID.yml 파일 내용 확인
