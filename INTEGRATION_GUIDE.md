# BrokerChain Wallet 功能集成指南

## 概述
本指南说明如何将contract项目中的功能集成到BrokerChain钱包Android应用中，采用分层导航设计：主界面 → 排行榜界面 → 具体功能界面。

## 界面结构

### 1. 主界面 (MainActivity)
- **新增按钮**: "勋章系统" - 进入勋章相关功能
- **位置**: 主界面第五行第一个位置

### 2. 勋章排行榜界面 (MedalRankingActivity)
- **功能**: 显示用户勋章排行榜，包括金牌、银牌、铜牌数量
- **导航按钮**: 
  - "📄 证明提交\n🎨 NFT铸造" - 进入合并功能界面
  - "🖼️ NFT查看" - 进入NFT查看界面
- **样式参考**: 前端项目的MedalRanking.vue组件

### 3. 证明提交 & NFT铸造 (ProofAndNFTActivity)
- **功能**: 合并的界面，包含两个标签页
- **标签页1 - 证明提交**:
  - 表单包含作者信息、事件类型、事件描述、贡献等级
  - 支持文件上传
  - 调用contract项目的API提交数据
- **标签页2 - NFT铸造**:
  - 支持两种图片类型：自动生成勋章、自定义图片
  - 文件选择和预览功能
  - 调用contract项目的API铸造NFT
- **样式参考**: 前端项目的MedalDistribution.vue和NFTMinting.vue组件

### 4. NFT查看 (NFTViewActivity)
- **功能**: 查看我的NFT和所有NFT
- **标签页切换**: 我的NFT / 所有NFT
- **实现**: 使用RecyclerView显示NFT列表，支持图片加载和显示

## 技术实现

### API集成
- **MedalApiUtil**: 处理勋章相关API调用
- **ProofApiUtil**: 处理证明材料提交API调用
- **NFTApiUtil**: 处理NFT相关API调用
- **基础URL**: `http://localhost:3001/api` (contract项目API地址)

### 数据模型
- **MedalRankingItem**: 排行榜数据模型
- **NFTItem**: NFT数据模型
- 参考contract项目的DTO类结构

### UI组件
- **MedalRankingAdapter**: 排行榜列表适配器
- **NFTViewAdapter**: NFT列表适配器
- 使用Glide加载图片
- 支持文件选择和预览

## 配置要求

### 1. 网络权限
确保AndroidManifest.xml中包含网络权限：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 2. 依赖库
确保build.gradle中包含必要依赖：
```gradle
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'com.github.bumptech.glide:glide:4.16.0'
implementation 'androidx.recyclerview:recyclerview:1.3.0'
```

### 3. contract项目配置
- 确保contract项目的后端服务运行在localhost:3001
- 确保API端点正确配置
- 确保CORS设置允许Android应用访问

## 使用说明

### 1. 启动contract项目
```bash
cd contract
npm install
npm start
```

### 2. 配置API地址
在API工具类中修改BASE_URL：
```java
private static final String BASE_URL = "http://your-server-ip:3001/api";
```

### 3. 测试功能
1. 启动Android应用
2. 在主界面点击新增的功能按钮
3. 测试各个功能的完整流程

## 文件结构

### 新增Java文件
```
app/src/main/java/com/example/brokerfi/xc/
├── MedalRankingActivity.java
├── ProofAndNFTActivity.java
├── NFTViewActivity.java
├── MedalApiUtil.java
├── ProofApiUtil.java
├── NFTApiUtil.java
└── adapter/
    ├── MedalRankingAdapter.java
    └── NFTViewAdapter.java
```

### 新增布局文件
```
app/src/main/res/layout/
├── activity_medal_ranking.xml
├── activity_proof_and_nft.xml
├── activity_nft_view.xml
├── item_medal_ranking.xml
└── item_nft_view.xml
```

### 新增资源文件
```
app/src/main/res/
├── drawable/
│   └── placeholder_image.xml
└── values/
    └── arrays.xml
```

## 注意事项

1. **网络连接**: 确保Android设备能够访问contract项目的API
2. **文件上传**: 文件大小限制在500KB以内
3. **错误处理**: 所有API调用都包含错误处理机制
4. **UI适配**: 布局已适配不同屏幕尺寸
5. **数据安全**: 私钥和敏感信息使用现有安全机制

## 后续优化

1. **缓存机制**: 可以添加数据缓存减少API调用
2. **离线支持**: 支持离线查看已缓存的数据
3. **推送通知**: 添加勋章获得和NFT铸造成功的通知
4. **分享功能**: 支持分享NFT和勋章成就
5. **搜索功能**: 在NFT查看中添加搜索和筛选功能
