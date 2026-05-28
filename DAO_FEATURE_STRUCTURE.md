# BrokerWallet Android应用 - DAO功能结构说明

## 📁 DAO功能概览

本文档专门描述Android应用中与DAO（去中心化自治组织）相关的所有代码和文件。DAO功能包括：勋章排行榜、证明材料提交、NFT展示、个人中心和全局统计。

**注意：** DAO功能界面已全面英文化，提供更好的国际化体验。

---

## 🗂️ DAO相关文件结构

```
brokerwallet-academic/
└── app/src/main/
    ├── java/com/example/brokerfi/xc/
    │   ├── MedalRankingActivity.java        # 勋章排行榜（核心）
    │   ├── ProofAndNFTActivity.java         # 证明提交与NFT铸造
    │   ├── MyCenterActivity.java            # 个人中心（我的勋章、提交历史、我的NFT）
    │   ├── GlobalStatsActivity.java         # 全局统计（全局勋章、全局NFT）
    │   │
    │   ├── adapter/                         # 列表适配器
    │   │   ├── MedalRankingAdapter.java     # 勋章排行榜适配器
    │   │   ├── NFTViewAdapter.java          # NFT列表适配器
    │   │   └── SubmissionHistoryAdapter.java # 提交历史适配器
    │   │
    │   ├── model/                           # 数据模型
    │   │   ├── SubmissionRecord.java        # 提交记录模型
    │   │   └── NFT.java                     # NFT数据模型
    │   │
    │   ├── dto/                             # 数据传输对象
    │   │   └── MedalQueryResult.java        # 勋章查询结果
    │   │
    │   └── util/                            # 工具类
    │       ├── ProofUploadUtil.java         # 证明上传工具
    │       ├── SubmissionUtil.java          # 提交工具
    │       ├── MedalApiUtil.java            # 勋章API工具
    │       └── NFTApiUtil.java              # NFT API工具
    │
    └── res/
        ├── layout/                          # 布局文件
        │   ├── activity_medal_ranking.xml   # 勋章排行榜布局
        │   ├── activity_proof_and_nft.xml   # 证明提交布局
        │   ├── activity_my_center.xml       # 个人中心布局
        │   ├── activity_global_stats.xml    # 全局统计布局
        │   ├── item_submission_history.xml  # 提交历史项布局
        │   └── dialog_nft_detail.xml        # NFT详情对话框布局
        │
        ├── drawable/                        # 图标资源
        │   ├── dao_team.xml                 # DAO团队图标
        │   └── dao_team_icon.xml            # DAO图标
        │
        └── values/
            └── strings.xml                  # 字符串资源（已英文化）
```

---

## 🔑 核心Activity说明

### 1. MedalRankingActivity.java - 勋章排行榜

**位置：** `app/src/main/java/com/example/brokerfi/xc/MedalRankingActivity.java`

**功能：**
- ✅ 显示全局勋章排行榜
- ✅ 显示我的排名和勋章
- ✅ 支持下拉刷新
- ✅ 点击用户卡片查看详情
- ✅ 显示用户昵称和代表作
- ✅ 计算说明弹窗（英文）

**关键功能：**

```java
// 查询全局排行榜
private void loadGlobalRanking() {
    String url = BaseUrl.getBaseUrl() + "/api/medals/ranking";
    OkhttpUtils.get(url, new MyCallBack() {
        @Override
        public void onSuccess(String result) {
            parseRankingData(result);
        }
    });
}

// 查询我的勋章
private void loadMyMedals() {
    String address = getMyAddress();
    String url = BaseUrl.getBaseUrl() + "/api/medals/user/" + address;
    OkhttpUtils.get(url, new MyCallBack() {
        @Override
        public void onSuccess(String result) {
            parseMyMedals(result);
        }
    });
}

// 显示计算说明对话框（英文）
private void showCalculationHelpDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("🏆 Medal Ranking Calculation");
    builder.setMessage(
        "📊 Score Calculation Formula:\n" +
        "Total Score = Gold × 3 + Silver × 2 + Bronze × 1\n\n" +
        "🥇 Gold Medal = 3 points\n" +
        "🥈 Silver Medal = 2 points\n" +
        "🥉 Bronze Medal = 1 point\n\n" +
        "📈 Ranking Rules:\n" +
        "1. Sorted by total score (descending)\n" +
        "2. If tied, sorted by gold medals\n" +
        "3. If tied, sorted by silver medals\n" +
        "4. If tied, sorted by bronze medals"
    );
    builder.setPositiveButton("Submit Now", ...);
    builder.show();
}
```

**布局文件：** `res/layout/activity_medal_ranking.xml`

**关键UI元素：**
- 顶部标题："🏆 Medal Ranking"
- 两个标签页："📊 Global" 和 "👤 My"
- 提交证明按钮："📄 Proof Submit"
- 计算说明按钮："❓"
- RecyclerView显示排行榜列表

---

### 2. ProofAndNFTActivity.java - 证明提交与NFT铸造

**位置：** `app/src/main/java/com/example/brokerfi/xc/ProofAndNFTActivity.java`

**功能：**
- ✅ 批量上传证明文件（支持多文件）
- ✅ 上传NFT图片（可选）
- ✅ 设置显示昵称
- ✅ 选择是否显示代表作
- ✅ 文件选择帮助说明（英文）
- ✅ 提交成功提示（英文）

**关键功能：**

```java
// 选择证明文件（支持多选）
private void selectProofFiles() {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("*/*");
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);  // 允许多选
    startActivityForResult(intent, REQUEST_CODE_PROOF_FILES);
}

// 选择NFT图片
private void selectNftImage() {
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setType("image/*");
    startActivityForResult(intent, REQUEST_CODE_NFT_IMAGE);
}

// 提交所有文件
private void submitAll() {
    String walletAddress = getMyAddress();
    String displayName = displayNameInput.getText().toString();
    boolean showWork = showWorkRadioGroup.getCheckedRadioButtonId() == R.id.showWorkYes;
    
    // 使用工具类上传
    ProofUploadUtil.uploadBatch(
        this,
        walletAddress,
        selectedProofFiles,
        selectedNftImageUri,
        displayName,
        showWork,
        new ProofUploadUtil.UploadCallback() {
            @Override
            public void onSuccess(String message) {
                showSuccessMessage(message);
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(ProofAndNFTActivity.this, error, Toast.LENGTH_LONG).show();
            }
        }
    );
}

// 显示成功消息（英文）
private void showSuccessMessage(String message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("✅ Submission Success");
    builder.setMessage(
        "Submission completed!\n\n" +
        "📄 Proof files uploaded, waiting for admin review\n" +
        "🖼️ NFT image uploaded (if provided)\n" +
        "⏳ Please wait for review results\n\n" +
        "You can check submission status in 'My Center'"
    );
    builder.setPositiveButton("OK", ...);
    builder.show();
}

// 文件选择帮助（英文）
private void showFileHelpDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("💡 File Selection Tips");
    builder.setMessage(
        "To select files from apps like WeChat, follow these steps:\n\n" +
        "1️⃣ Find the file you want to upload in WeChat\n" +
        "2️⃣ Long press the file, select 「Forward」\n" +
        "3️⃣ Choose 「Save to Files」 or 「More」\n" +
        "4️⃣ Save the file to phone storage\n" +
        "5️⃣ Return to this page, click 「Select Proof File」 to find the saved file"
    );
    builder.show();
}
```

**布局文件：** `res/layout/activity_proof_and_nft.xml`

**关键UI元素：**
- 标题："📄 Proof Submission"
- 证明文件选择："Submit Proof File *"
- NFT图片选择："Select Photo for NFT Minting (Optional)"
- 昵称输入框："Enter your preferred display nickname"
- 代表作显示选项："Yes" / "No"
- 提交按钮："Submit Proof"

---

### 3. MyCenterActivity.java - 个人中心

**位置：** `app/src/main/java/com/example/brokerfi/xc/MyCenterActivity.java`

**功能：**
- ✅ 显示我的勋章统计
- ✅ 显示提交历史（分页加载）
- ✅ 显示我的NFT（分页加载）
- ✅ 地址切换时自动清空缓存
- ✅ 支持下拉刷新
- ✅ NFT详情查看（无关闭按钮）

**关键功能：**

```java
// 静态缓存变量
private static List<SubmissionRecord> cachedSubmissionList = new ArrayList<>();
private static List<NFT> cachedNftList = new ArrayList<>();
private static String cachedWalletAddress = null;  // 缓存的钱包地址

// 检查并恢复NFT缓存（地址切换检测）
private void checkAndRestoreNftCache() {
    String currentAddress = getMyAddressForDatabase();
    
    if (cachedWalletAddress != null && cachedWalletAddress.equals(currentAddress)) {
        // 地址未变，恢复缓存
        if (cachedNftList != null && !cachedNftList.isEmpty()) {
            nftList.clear();
            nftList.addAll(cachedNftList);
            nftHasMore = cachedNftHasMore;
            totalNftCount = cachedTotalNftCount;
            Log.d("MyCenter", "Address unchanged, restored NFT cache: " + nftList.size());
        }
    } else {
        // 地址改变，清空旧缓存
        if (cachedWalletAddress != null) {
            Log.d("MyCenter", "Address changed from " + cachedWalletAddress + 
                  " to " + currentAddress + ", clearing cache");
        }
        clearNftCache();
    }
}

// 清空NFT缓存
private void clearNftCache() {
    cachedNftList = new ArrayList<>();
    cachedNftHasMore = true;
    cachedTotalNftCount = 0;
    cachedWalletAddress = null;
    Log.d("MyCenter", "NFT cache cleared");
}

// 保存NFT缓存
private void saveNftCache() {
    cachedNftList = new ArrayList<>(nftList);
    cachedNftHasMore = nftHasMore;
    cachedTotalNftCount = totalNftCount;
    cachedWalletAddress = getMyAddressForDatabase();  // 保存当前地址
    Log.d("MyCenter", "NFT cache saved: " + cachedNftList.size() + " items, address=" + cachedWalletAddress);
}

// 加载我的勋章
private void loadMyMedals() {
    String address = getMyAddressForDatabase();
    String url = BaseUrl.getBaseUrl() + "/api/medals/user/" + address;
    OkhttpUtils.get(url, new MyCallBack() {
        @Override
        public void onSuccess(String result) {
            parseMyMedals(result);
        }
    });
}

// 加载提交历史（分页）
private void loadSubmissionHistory(int page) {
    String address = getMyAddressForDatabase();
    String url = BaseUrl.getBaseUrl() + "/api/files/submission-history?walletAddress=" + 
                 address + "&page=" + page + "&size=" + PAGE_SIZE;
    OkhttpUtils.get(url, new MyCallBack() {
        @Override
        public void onSuccess(String result) {
            parseSubmissionHistory(result, page);
        }
    });
}

// 加载我的NFT（分页）
private void loadMyNfts(int page) {
    String address = getMyAddressForDatabase();
    String url = BaseUrl.getBaseUrl() + "/api/blockchain/nfts/user/" + 
                 address + "?page=" + page + "&size=" + PAGE_SIZE;
    OkhttpUtils.get(url, new MyCallBack() {
        @Override
        public void onSuccess(String result) {
            parseMyNfts(result, page);
        }
    });
}

// 显示NFT详情（无关闭按钮）
private void showNftDetail(NFT nft) {
    Dialog dialog = new Dialog(this);
    dialog.setContentView(R.layout.dialog_nft_detail);
    
    // 设置NFT信息
    ImageView nftImage = dialog.findViewById(R.id.nftImageView);
    TextView uploadTimeText = dialog.findViewById(R.id.uploadTimeText);
    TextView mintTimeText = dialog.findViewById(R.id.mintTimeText);
    // ... 其他UI元素
    
    // 加载图片
    Glide.with(this).load(nft.getImageUrl()).into(nftImage);
    
    // 点击外部或返回键关闭
    dialog.setCanceledOnTouchOutside(true);
    dialog.show();
}
```

**布局文件：** `res/layout/activity_my_center.xml`

**关键UI元素：**
- 标题："👤 My"
- 勋章统计卡片："🏆 My Medals"
  - 金牌数："Gold: X"
  - 银牌数："Silver: X"
  - 铜牌数："Bronze: X"
  - 总分："Total: X"
- 提交历史标签："📝 Submission History"
- NFT标签："🖼️ My NFTs"
- 空状态提示："No Submission History" / "No NFTs yet"

---

### 4. GlobalStatsActivity.java - 全局统计

**位置：** `app/src/main/java/com/example/brokerfi/xc/GlobalStatsActivity.java`

**功能：**
- ✅ 显示全局勋章统计
- ✅ 显示全局NFT画廊（分页加载）
- ✅ 支持下拉刷新
- ✅ NFT详情查看

**关键功能：**

```java
// 加载全局统计
private void loadGlobalStats() {
    String url = BaseUrl.getBaseUrl() + "/api/medals/global-stats";
    OkhttpUtils.get(url, new MyCallBack() {
        @Override
        public void onSuccess(String result) {
            parseGlobalStatsData(result);
        }
    });
}

// 解析全局统计数据（英文显示）
private void parseGlobalStatsData(String result) {
    JSONObject data = new JSONObject(result);
    
    totalUsersText.setText("Total Users: " + data.optInt("totalUsers", 0));
    highestScoreText.setText("Highest Score: " + data.optInt("highestScore", 0));
    
    String topUser = data.optString("topUserDisplayName", "None");
    if (topUser.equals("null") || topUser.isEmpty()) {
        topUser = "None";
    }
    topUserText.setText("Top User: " + topUser);
    
    goldCountText.setText(String.valueOf(data.optInt("totalGold", 0)));
    silverCountText.setText(String.valueOf(data.optInt("totalSilver", 0)));
    bronzeCountText.setText(String.valueOf(data.optInt("totalBronze", 0)));
}

// 加载全局NFT（分页）
private void loadGlobalNfts(int page) {
    String url = BaseUrl.getBaseUrl() + "/api/blockchain/nfts/all?page=" + 
                 page + "&size=" + PAGE_SIZE;
    OkhttpUtils.get(url, new MyCallBack() {
        @Override
        public void onSuccess(String result) {
            parseGlobalNfts(result, page);
        }
    });
}
```

**布局文件：** `res/layout/activity_global_stats.xml`

**关键UI元素：**
- 标题："📊 Global Stats"
- 全局勋章统计卡片："🏆 Global Medal Stats"
  - 总用户数："Total Users: X"
  - 最高分："Highest Score: X"
  - 榜首用户："Top User: XXX"
  - 金银铜牌总数
- NFT画廊："🎨 Global NFT Gallery"
- 空状态："No NFTs minted globally yet!"

---

## 📦 适配器说明

### 1. MedalRankingAdapter.java - 勋章排行榜适配器

**位置：** `app/src/main/java/com/example/brokerfi/xc/adapter/MedalRankingAdapter.java`

**功能：**
- ✅ 显示用户排名卡片
- ✅ 显示用户昵称（或"Anonymous"）
- ✅ 显示勋章数量和总分
- ✅ 显示代表作品（如果有）

**关键代码：**
```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    MedalRankingItem item = rankingList.get(position);
    
    // 排名
    holder.rankText.setText(String.valueOf(item.getRank()));
    
    // 昵称（英文）
    holder.displayNameText.setText(
        item.getDisplayName() != null && !item.getDisplayName().isEmpty() 
        ? item.getDisplayName() 
        : "Anonymous"
    );
    
    // 勋章数量
    holder.goldText.setText(String.valueOf(item.getGoldMedals()));
    holder.silverText.setText(String.valueOf(item.getSilverMedals()));
    holder.bronzeText.setText(String.valueOf(item.getBronzeMedals()));
    
    // 总分（英文）
    holder.totalMedalText.setText("Total: " + item.getTotalMedalScore());
    
    // 代表作（英文）
    if (item.isShowRepresentativeWork() && item.getRepresentativeWork() != null) {
        holder.representativeWorkText.setVisibility(View.VISIBLE);
        holder.representativeWorkText.setText("Work: " + item.getRepresentativeWork());
    } else {
        holder.representativeWorkText.setVisibility(View.GONE);
    }
}
```

---

### 2. NFTViewAdapter.java - NFT列表适配器

**位置：** `app/src/main/java/com/example/brokerfi/xc/adapter/NFTViewAdapter.java`

**功能：**
- ✅ 显示NFT图片（使用Glide加载）
- ✅ 显示时间信息（英文）
- ✅ 显示持有者信息
- ✅ 支持分页加载（Footer显示加载状态）

**关键代码：**
```java
@Override
public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof ViewHolder) {
        NFT nft = nftList.get(position);
        ViewHolder vh = (ViewHolder) holder;
        
        // 加载NFT图片
        Glide.with(context)
            .load(nft.getImageUrl())
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)
            .into(vh.nftImageView);
        
        // 时间信息（英文）
        String uploadTime = nft.getUploadTime() != null ? nft.getUploadTime() : "Unknown";
        String mintTime = nft.getMintTime() != null ? nft.getMintTime() : "Unknown";
        
        vh.uploadTimeText.setText("Material Upload: " + uploadTime);
        vh.mintTimeText.setText("NFT Minted: " + mintTime);
        
        // 持有者信息（英文）
        String ownerAddress = nft.getOwnerAddress();
        String shortAddress = ownerAddress.substring(0, 6) + "..." + 
                             ownerAddress.substring(ownerAddress.length() - 4);
        vh.ownerAddressText.setText("Owner Address: " + shortAddress);
        
        String ownerDisplayName = nft.getOwnerDisplayName() != null && 
                                 !nft.getOwnerDisplayName().isEmpty()
                                 ? nft.getOwnerDisplayName() 
                                 : "Anonymous";
        vh.ownerDisplayNameText.setText("Owner Nickname: " + ownerDisplayName);
        
        // 点击查看详情
        vh.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(nft);
            }
        });
    } else if (holder instanceof FooterViewHolder) {
        FooterViewHolder fh = (FooterViewHolder) holder;
        
        // Footer状态（英文）
        if (isLoading) {
            fh.footerText.setText("Loading...");
            fh.progressBar.setVisibility(View.VISIBLE);
        } else if (hasMore) {
            fh.footerText.setText("Pull up to load more");
            fh.progressBar.setVisibility(View.GONE);
        } else {
            fh.footerText.setText("End of list ~ Submit materials to get more NFTs");
            fh.progressBar.setVisibility(View.GONE);
        }
    }
}
```

---

### 3. SubmissionHistoryAdapter.java - 提交历史适配器

**位置：** `app/src/main/java/com/example/brokerfi/xc/adapter/SubmissionHistoryAdapter.java`

**功能：**
- ✅ 显示提交记录卡片
- ✅ 显示文件列表
- ✅ 显示审核状态（英文）
- ✅ 显示勋章信息（英文）
- ✅ 显示进度条（英文）

**关键代码：**
```java
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    SubmissionRecord record = recordList.get(position);
    
    // 文件列表（英文）
    List<String> fileNames = record.getFileNames();
    if (fileNames != null && !fileNames.isEmpty()) {
        String firstFile = fileNames.get(0);
        if (fileNames.size() > 1) {
            holder.fileNameText.setText(firstFile + " and " + 
                                       (fileNames.size() - 1) + " more file(s)");
        } else {
            holder.fileNameText.setText(firstFile);
        }
    }
    
    // 审核状态（英文）
    String status = record.getAuditStatus();
    if ("APPROVED".equals(status)) {
        holder.statusText.setText("Approved");
        holder.statusText.setTextColor(Color.parseColor("#4CAF50"));
    } else if ("REJECTED".equals(status)) {
        holder.statusText.setText("Rejected");
        holder.statusText.setTextColor(Color.parseColor("#F44336"));
    } else {
        holder.statusText.setText("Pending");
        holder.statusText.setTextColor(Color.parseColor("#FF9800"));
    }
    
    // 勋章信息（英文）
    String medalInfo = buildMedalInfo(record.getMedalAwarded());
    holder.medalText.setText(medalInfo);
    
    // 进度条（英文）
    updateProgress(holder, record);
}

// 构建勋章信息（英文）
private String buildMedalInfo(String medalAwarded) {
    if (medalAwarded == null || "NONE".equals(medalAwarded)) {
        return "⚪ No Medal Awarded";
    } else {
        return "🏅 Medal Awarded";
    }
}

// 更新进度（英文）
private void updateProgress(ViewHolder holder, SubmissionRecord record) {
    String progressStr = "1/3 Uploaded";
    int progress = 33;
    
    if ("APPROVED".equals(record.getAuditStatus())) {
        progressStr = "2/3 Approved";
        progress = 66;
        
        if (record.getMedalAwarded() != null && !"NONE".equals(record.getMedalAwarded())) {
            if (record.isHasNftImage()) {
                progressStr = "3/3 NFT Minted";
            } else {
                progressStr = "3/3 Medal Awarded";
            }
            progress = 100;
        }
    } else if ("REJECTED".equals(record.getAuditStatus())) {
        progressStr = "Audit Rejected";
        progress = 0;
    }
    
    holder.progressText.setText(progressStr);
    holder.progressBar.setProgress(progress);
}
```

---

## 🎨 布局文件说明

### 1. dialog_nft_detail.xml - NFT详情对话框

**位置：** `res/layout/dialog_nft_detail.xml`

**重要变更：**
- ✅ 移除了底部的"Close"按钮
- ✅ 用户可以点击外部区域或返回键关闭对话框
- ✅ 所有文本已英文化

**关键元素：**
```xml
<LinearLayout>
    <!-- NFT图片 -->
    <TextView android:text="NFT Image" />
    <ImageView android:id="@+id/nftImageView" />
    
    <!-- 时间信息 -->
    <TextView android:text="Time Information" />
    <TextView android:id="@+id/uploadTimeText" 
              android:text="Material Upload: 2025-10-10" />
    <TextView android:id="@+id/mintTimeText" 
              android:text="NFT Minted: 2025-10-10" />
    
    <!-- 持有者信息 -->
    <TextView android:id="@+id/ownerAddressText" 
              android:text="Owner Address: 0x..." />
    <TextView android:id="@+id/ownerDisplayNameText" 
              android:text="Owner Nickname: Anonymous" />
    
    <!-- 已移除Close按钮 -->
</LinearLayout>
```

---

### 2. item_submission_history.xml - 提交历史项布局

**位置：** `res/layout/item_submission_history.xml`

**关键元素（已英文化）：**
```xml
<androidx.cardview.widget.CardView>
    <LinearLayout>
        <!-- 文件名 -->
        <TextView android:id="@+id/fileNameText" 
                  android:text="file.pdf and 2 more file(s)" />
        
        <!-- 审核状态 -->
        <TextView android:id="@+id/statusText" 
                  android:text="Pending" />
        
        <!-- 勋章信息 -->
        <TextView android:id="@+id/medalText" 
                  android:text="⚪ No Medal" />
        
        <!-- NFT状态 -->
        <TextView android:id="@+id/nftStatusText" 
                  android:text="🖼️ Not Started" />
        
        <!-- 代币奖励 -->
        <TextView android:id="@+id/tokenRewardText" 
                  android:text="💰 BKC Reward: 10.5 BKC" />
        
        <!-- 进度条 -->
        <ProgressBar android:id="@+id/progressBar" />
        <TextView android:id="@+id/progressText" 
                  android:text="1/3 Uploaded" />
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

---

## 🔧 工具类说明

### 1. ProofUploadUtil.java - 证明上传工具

**位置：** `app/src/main/java/com/example/brokerfi/xc/ProofUploadUtil.java`

**功能：**
- ✅ 批量上传证明文件
- ✅ 上传NFT图片
- ✅ 生成批次ID
- ✅ 错误处理（英文）

**关键方法：**
```java
// 批量上传
public static void uploadBatch(
    Context context,
    String walletAddress,
    List<Uri> proofFiles,
    Uri nftImageUri,
    String displayName,
    boolean showWork,
    UploadCallback callback
) {
    // 1. 生成批次ID
    String batchId = generateBatchId();
    
    // 2. 上传证明文件
    uploadProofFiles(context, walletAddress, proofFiles, batchId, ...);
    
    // 3. 上传NFT图片（如果有）
    if (nftImageUri != null) {
        uploadNftImage(context, walletAddress, nftImageUri, batchId, ...);
    }
    
    // 4. 回调成功
    callback.onSuccess("Submission completed!");
}

// 错误消息解析（英文）
private static String parseErrorMessage(String errorBody, int statusCode) {
    try {
        JSONObject jsonError = new JSONObject(errorBody);
        
        // 检查是否是重复NFT图片错误
        if (jsonError.has("errorCode") && 
            "DUPLICATE_NFT_IMAGE".equals(jsonError.optString("errorCode"))) {
            return "NFT image uniqueness constraint: This NFT already exists, " +
                   "please select a different image to mint";
        }
        
        // 其他错误
        if (jsonError.has("message")) {
            return jsonError.getString("message");
        }
    } catch (Exception e) {
        // 解析失败，返回默认错误消息
    }
    
    // 根据状态码返回默认消息（英文）
    if (statusCode == 400) {
        return "Upload failed, please check file format and content";
    } else if (statusCode == 500) {
        return "Server error, please try again later";
    } else {
        return "Upload failed (Error code: " + statusCode + ")";
    }
}
```

---

## 🌐 字符串资源（英文化）

### strings.xml

**位置：** `res/values/strings.xml`

**DAO相关字符串（已英文化）：**
```xml
<resources>
    <!-- 勋章排行榜 -->
    <string name="medal_ranking">Medal Ranking</string>
    <string name="global_ranking">Global</string>
    <string name="my_ranking">My</string>
    <string name="gold_medal">Gold</string>
    <string name="silver_medal">Silver</string>
    <string name="bronze_medal">Bronze</string>
    <string name="total_score">Total</string>
    
    <!-- 证明提交 -->
    <string name="proof_submission">Proof Submission</string>
    <string name="select_proof_file">Submit Proof File</string>
    <string name="select_nft_image">Select Photo for NFT Minting (Optional)</string>
    <string name="display_name_hint">Enter your preferred display nickname</string>
    <string name="show_representative_work">Display representative work on ranking</string>
    <string name="submit_proof">Submit Proof</string>
    
    <!-- 个人中心 -->
    <string name="my_center">My</string>
    <string name="my_medals">My Medals</string>
    <string name="submission_history">Submission History</string>
    <string name="my_nfts">My NFTs</string>
    <string name="no_submission_history">No Submission History</string>
    <string name="no_nfts">No NFTs yet</string>
    
    <!-- 全局统计 -->
    <string name="global_stats">Global Stats</string>
    <string name="global_medal_stats">Global Medal Stats</string>
    <string name="total_users">Total Users</string>
    <string name="highest_score">Highest Score</string>
    <string name="top_user">Top User</string>
    <string name="global_nft_gallery">Global NFT Gallery</string>
    
    <!-- 状态 -->
    <string name="pending">Pending</string>
    <string name="approved">Approved</string>
    <string name="rejected">Rejected</string>
    <string name="loading">Loading...</string>
    <string name="no_data">No Data</string>
</resources>
```

---

## 🔄 核心业务流程

### 1. 证明材料提交流程

```
用户打开 ProofAndNFTActivity
         ↓
    点击"Select Proof File" → 选择多个文件
         ↓
    （可选）点击"Select Photo for NFT Minting" → 选择图片
         ↓
    输入昵称、选择是否显示代表作
         ↓
    点击"Submit Proof"
         ↓
    ProofUploadUtil.uploadBatch()
         ↓
    生成批次ID (timestamp + random)
         ↓
    上传证明文件到后端 /api/files/upload-batch
         ↓
    （如果有）上传NFT图片到后端 /api/files/upload-nft-image
         ↓
    显示成功消息（英文）
         ↓
    用户可在 MyCenterActivity 查看提交历史
```

### 2. 地址切换缓存清理流程

```
用户切换钱包账户
         ↓
    打开 MyCenterActivity
         ↓
    调用 checkAndRestoreNftCache()
         ↓
    获取当前钱包地址
         ↓
    比较 cachedWalletAddress 和当前地址
         ↓
    如果地址相同 → 恢复缓存
         ↓
    如果地址不同 → 调用 clearNftCache()
         ↓
    清空所有静态缓存变量
         ↓
    重新加载数据
         ↓
    保存新地址和新数据到缓存
```

### 3. NFT分页加载流程

```
打开 MyCenterActivity 或 GlobalStatsActivity
         ↓
    初始化 page = 0, size = 10
         ↓
    调用 loadMyNfts(0) 或 loadGlobalNfts(0)
         ↓
    请求 API: /api/blockchain/nfts/user/{address}?page=0&size=10
         ↓
    后端从区块链查询NFT（倒序）
         ↓
    返回 JSON: { nfts: [...], hasMore: true, total: 50 }
         ↓
    解析数据，添加到 nftList
         ↓
    更新 NFTViewAdapter
         ↓
    用户滑动到底部 → 触发加载更多
         ↓
    page++, 调用 loadMyNfts(1)
         ↓
    追加新数据到列表
         ↓
    直到 hasMore = false → 显示"End of list"
```

---

## 🛠️ 技术栈

### 核心框架
- **Android SDK** - 原生Android开发
- **Java** - 编程语言

### UI组件
- **RecyclerView** - 列表展示
- **CardView** - 卡片布局
- **SwipeRefreshLayout** - 下拉刷新
- **AlertDialog** - 对话框

### 网络请求
- **OkHttp** - HTTP客户端
- **JSON** - 数据解析

### 图片加载
- **Glide** - 图片加载和缓存

### 其他
- **SharedPreferences** - 本地数据存储
- **Intent** - 页面跳转和文件选择

---

## 📝 配置说明

### 服务器地址配置

**位置：** `app/src/main/java/com/example/brokerfi/config/ServerConfig.java`

```java
public class ServerConfig {
    // 本地开发（USB调试）
    public static final String BASE_URL = "http://192.168.1.100:5000";
    
    // 云服务器部署
    // public static final String BASE_URL = "http://your-domain.com:5000";
}
```

**⚠️ 重要：** 部署到云服务器时需要修改此配置！

---

## 🚀 构建与运行

### 开发环境运行

```bash
# 1. 打开Android Studio
# 2. 导入项目：brokerwallet-academic
# 3. 连接手机（开启USB调试）
# 4. 点击运行按钮

# 或使用命令行
cd brokerwallet-academic
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### USB调试端口转发

```bash
# 将手机的5000端口转发到电脑的5000端口
adb reverse tcp:5000 tcp:5000

# 验证
adb reverse --list
```

---

## 🆘 常见问题

### Q1: 网络请求失败
**解决：** 
1. 检查后端是否启动
2. 检查 `ApiConfig.BASE_URL` 是否正确
3. 确保执行了 `adb reverse tcp:5000 tcp:5000`

### Q2: 图片加载失败
**解决：** 
1. 检查图片URL是否正确
2. 检查网络权限
3. 查看Glide错误日志

### Q3: 地址切换后数据未刷新
**解决：** 
1. 检查 `checkAndRestoreNftCache()` 是否被调用
2. 查看日志确认缓存是否被清空
3. 手动下拉刷新

### Q4: 文件选择失败
**解决：** 
1. 检查存储权限
2. 使用"文件选择帮助"提示的方法
3. 尝试从相册或文件管理器选择

---

## 📚 相关文档

- **项目总览：** `../../PROJECT_STRUCTURE.md`
- **后端文档：** `../../BrokerWallet-backend/PROJECT_STRUCTURE.md`
- **前端文档：** `../../brokerwallet-frontend/PROJECT_STRUCTURE.md`
- **部署指南：** `../../DEPLOYMENT_GUIDE.md`

---

**最后更新：** 2025年10月10日

