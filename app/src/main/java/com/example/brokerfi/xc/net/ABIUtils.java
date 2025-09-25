package com.example.brokerfi.xc.net;

import android.util.Log;

import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ABIUtils {

    private static final String MINT_SELECTOR = "d41d9a27";
    private static final String LIST_SELECTOR = "b0136c8d";
    private static final String UNLIST_SELECTOR = "961f0944";
    private static final String GET_MY_NFTS_SELECTOR = "629cb2e4";
    private static final String GET_LISTED_NFTS_SELECTOR = "a3f7f7d8";
    private static final String BUY_SELECTOR = "1d85bf03";
    
    // 勋章系统选择器
    private static final String GET_USER_MEDALS_SELECTOR = "5ef4daa5";  // getUserMedals(address)
    private static final String GET_GLOBAL_STATS_SELECTOR = "fa55312b";  // getGlobalStats()
    
    // NFT查询选择器
    private static final String GET_USER_NFTS_SELECTOR = "i9j0k1l2";
    private static final String GET_NFT_DATA_SELECTOR = "m3n4o5p6";



    public static String encodeMint(String name, String base64Image, BigInteger shares) {

        int baseOffset = 32 * 3;
        int nameContentLength = 32 + ((name.getBytes().length + 31) / 32) * 32;
        int imageOffset = baseOffset + nameContentLength;

        StringBuilder data = new StringBuilder();

        data.append(MINT_SELECTOR);

        data.append(padLeft(Integer.toHexString(baseOffset), 64));

        data.append(padLeft(Integer.toHexString(imageOffset), 64));

        data.append(padLeft(shares.toString(16), 64));


        data.append(encodeDynamicString(name));
        data.append(encodeDynamicString(base64Image));

        return "0x" + data.toString();
    }

    private static String encodeDynamicString(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        String length = padLeft(Integer.toHexString(bytes.length), 64);

        String content = Numeric.toHexStringNoPrefix(bytes);

        int totalBytes = ((bytes.length + 31) / 32) * 32;
        content = padRight(content, totalBytes * 2);

        return length + content;
    }

    public static String encodeList(BigInteger nftId, BigInteger shares, BigInteger price) {
        StringBuilder data = new StringBuilder();
        data.append(LIST_SELECTOR);
        data.append(padLeft(nftId.toString(16), 64));
        data.append(padLeft(shares.toString(16), 64));
        data.append(padLeft(price.toString(16), 64));

        return "0x" + data.toString();
    }

    public static String encodeUnlist(BigInteger listingId) {
        StringBuilder data = new StringBuilder();
        data.append(UNLIST_SELECTOR);
        data.append(padLeft(listingId.toString(16), 64));

        return "0x" + data.toString();
    }

    public static String encodeBuy(BigInteger listingId, BigInteger shares) {
        StringBuilder data = new StringBuilder();
        data.append(BUY_SELECTOR);
        data.append(padLeft(listingId.toString(16), 64));
        data.append(padLeft(shares.toString(16), 64));

        return "0x" + data.toString();
    }

    public static String encodeGetMyNFTs() {
        return "0x" + GET_MY_NFTS_SELECTOR;
    }

    public static String encodeGetListedNFTs() {
        return "0x" + GET_LISTED_NFTS_SELECTOR;
    }
    
    // 勋章查询编码方法
    public static String encodeGetUserMedals(String address) {
        StringBuilder data = new StringBuilder();
        data.append(GET_USER_MEDALS_SELECTOR);
        data.append(padLeft(address.substring(2), 64)); // 移除0x前缀并填充到64字符
        return "0x" + data.toString();
    }
    
    public static String encodeGetGlobalStats() {
        return "0x" + GET_GLOBAL_STATS_SELECTOR;
    }
    
    // NFT查询编码方法
    public static String encodeGetUserNfts(String address) {
        StringBuilder data = new StringBuilder();
        data.append(GET_USER_NFTS_SELECTOR);
        data.append(padLeft(address.substring(2), 64)); // 移除0x前缀并填充到64字符
        return "0x" + data.toString();
    }
    
    public static String encodeGetNftData(BigInteger tokenId) {
        StringBuilder data = new StringBuilder();
        data.append(GET_NFT_DATA_SELECTOR);
        data.append(padLeft(tokenId.toString(16), 64));
        return "0x" + data.toString();
    }


    private static String padLeft(String s, int length) {
        if (s.length() >= length) return s;
        return String.format("%" + length + "s", s).replace(' ', '0');
    }

    private static String padRight(String s, int targetLength) {
        if (s.length() >= targetLength) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < targetLength) {
            sb.append('0');
        }
        return sb.toString();
    }



    public static class MyNFTsResult {
        public BigInteger[] nftIds;
        public String[] names;
        public String[] images;
        public BigInteger[] sharesList;
        public BigInteger[] pricesList;
        public boolean[] listingStatus;
        public BigInteger[] listingIds;
    }

    public static MyNFTsResult decodeGetMyNFTs(String hexResponse) {

        MyNFTsResult result = new MyNFTsResult();
        try {

            String cleanHex = hexResponse.startsWith("0x") ? hexResponse.substring(2) : hexResponse;
            if (cleanHex.isEmpty()) {

                return result;
            }
            byte[] data = Numeric.hexStringToByteArray(cleanHex);


            // 1. 解析7个动态数组的偏移量
            int[] arrayOffsets = new int[7];
            for (int i = 0; i < 7; i++) {
                arrayOffsets[i] = Numeric.toBigInt(data, i * 32, 32).intValue();
            }


            result.nftIds = decodeUint256Array(data, arrayOffsets[0]);
            result.names = decodeStringArray(data, arrayOffsets[1]);
            result.images = decodeStringArray(data, arrayOffsets[2]);
            result.sharesList = decodeUint256Array(data, arrayOffsets[3]);
            result.pricesList = decodeUint256Array(data, arrayOffsets[4]);
            result.listingStatus = decodeBoolArray(data, arrayOffsets[5]);
            result.listingIds = decodeUint256Array(data, arrayOffsets[6]);


        } catch (Exception e) {
            Log.e("ABI_DECODE", "解码失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }


    public static class ListedNFTsResult {

        public String[] addressList;
        public BigInteger[] nftIds;
        public String[] names;
        public String[] images;
        public BigInteger[] sharesList;
        public BigInteger[] pricesList;
        public BigInteger[] listingIds;
    }

    public static ListedNFTsResult decodeGetListedNFTs(String hexResponse) {

        ListedNFTsResult result = new ListedNFTsResult();
        try {

            String cleanHex = hexResponse.startsWith("0x") ? hexResponse.substring(2) : hexResponse;
            if (cleanHex.isEmpty()) {
                Log.e("ABI_DECODE", "空响应数据");
                return result;
            }
            byte[] data = Numeric.hexStringToByteArray(cleanHex);
            Log.d("ABI_DECODE", "字节数组长度: " + data.length);


            int[] arrayOffsets = new int[7];
            for (int i = 0; i < 7; i++) {
                arrayOffsets[i] = Numeric.toBigInt(data, i * 32, 32).intValue();
            }


            result.addressList = decodeAddressArray(data, arrayOffsets[0]);
            result.nftIds = decodeUint256Array(data, arrayOffsets[1]);
            result.names = decodeStringArray(data, arrayOffsets[2]);
            result.images = decodeStringArray(data, arrayOffsets[3]);
            result.sharesList = decodeUint256Array(data, arrayOffsets[4]);
            result.pricesList = decodeUint256Array(data, arrayOffsets[5]);
            result.listingIds = decodeUint256Array(data, arrayOffsets[6]);

            Log.d("ABI_DECODE", "addressList: " + Arrays.toString(result.addressList));
            Log.d("ABI_DECODE", "nftIds: " + Arrays.toString(result.nftIds));
            Log.d("ABI_DECODE", "names: " + Arrays.toString(result.names));
            Log.d("ABI_DECODE", "images: " + Arrays.toString(result.images));
            Log.d("ABI_DECODE", "sharesList: " + Arrays.toString(result.sharesList));
            Log.d("ABI_DECODE", "pricesList: " + Arrays.toString(result.pricesList));
            Log.d("ABI_DECODE", "listingIds: " + Arrays.toString(result.listingIds));

        } catch (Exception e) {
            Log.e("ABI_DECODE", "解码失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }


    private static String[] decodeStringArray(byte[] data, int offset) {
        int arrayLength = Numeric.toBigInt(data, offset, 32).intValue();
        String[] result = new String[arrayLength];

        for (int i = 0; i < arrayLength; i++) {

            int elementOffset = offset + 32 + (i * 32);
            int stringOffset = Numeric.toBigInt(data, elementOffset, 32).intValue();


            int absoluteStringOffset = offset + 32 + stringOffset;


            result[i] = decodeStringStrict(data, absoluteStringOffset);
        }
        return result;
    }

    private static String decodeStringStrict(byte[] data, int offset) {

        if (offset + 32 > data.length) {
            Log.e("DECODE_ERROR", "长度头越界！需要读取32字节，但剩余数据只有" + (data.length - offset) + "字节");
            return "";
        }


        int length = Numeric.toBigInt(data, offset, 32).intValue();
        int contentStart = offset + 32;


        if (contentStart + length > data.length) {
            Log.e("DECODE_ERROR", "内容越界！需要读取" + length + "字节，但剩余数据只有" + (data.length - contentStart) + "字节");
            return "";
        }

        byte[] realContent = Arrays.copyOfRange(data, contentStart, contentStart + length);

        Log.d("DECODE_HEX", "原始字节(hex): " + Numeric.toHexString(realContent));


        String result = new String(realContent, StandardCharsets.UTF_8);
        return result;
    }

    private static BigInteger[] decodeUint256Array(byte[] data, int offset) {
        int arrayLength = Numeric.toBigInt(data, offset, 32).intValue();
        BigInteger[] array = new BigInteger[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            array[i] = Numeric.toBigInt(data, offset + 32 + i * 32, 32);
        }
        return array;
    }

    private static boolean[] decodeBoolArray(byte[] data, int offset) {
        int arrayLength = Numeric.toBigInt(data, offset, 32).intValue();
        boolean[] array = new boolean[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            array[i] = Numeric.toBigInt(data, offset + 32 + i * 32, 32).intValue() != 0;
        }
        return array;
    }

    private static String[] decodeAddressArray(byte[] data, int offset) {
        int arrayLength = Numeric.toBigInt(data, offset, 32).intValue();
        String[] array = new String[arrayLength];
        for (int i = 0; i < arrayLength; i++) {

            String fullAddress = Numeric.toHexStringNoPrefix(
                    Arrays.copyOfRange(data, offset + 32 + i * 32, offset + 32 + i * 32 + 32)
            );

            array[i] = fullAddress.substring(24);
        }
        return array;
    }
    
    // 勋章查询结果类
    public static class MedalQueryResult {
        public int goldMedals;
        public int silverMedals;
        public int bronzeMedals;
        public int totalScore;
    }
    
    public static class GlobalStatsResult {
        public int totalUsers;
        public int totalGoldMedals;
        public int totalSilverMedals;
        public int totalBronzeMedals;
        public int totalMedals;
    }
    
    // NFT查询结果类
    public static class UserNftsResult {
        public BigInteger[] tokenIds;
        public String[] names;
        public String[] descriptions;
        public String[] imageUrls;
    }
    
    public static class NftDataResult {
        public String name;
        public String description;
        public String imageUrl;
        public String owner;
    }
    
    // 勋章查询解码方法
    public static MedalQueryResult decodeGetUserMedals(String hexResponse) {
        MedalQueryResult result = new MedalQueryResult();
        try {
            String cleanHex = hexResponse.startsWith("0x") ? hexResponse.substring(2) : hexResponse;
            if (cleanHex.isEmpty()) {
                return result;
            }
            byte[] data = Numeric.hexStringToByteArray(cleanHex);
            
            result.goldMedals = Numeric.toBigInt(data, 0, 32).intValue();
            result.silverMedals = Numeric.toBigInt(data, 32, 32).intValue();
            result.bronzeMedals = Numeric.toBigInt(data, 64, 32).intValue();
            result.totalScore = Numeric.toBigInt(data, 96, 32).intValue();
        } catch (Exception e) {
            Log.e("ABI_DECODE", "解码勋章数据失败: " + e.getMessage());
        }
        return result;
    }
    
    public static GlobalStatsResult decodeGetGlobalStats(String hexResponse) {
        GlobalStatsResult result = new GlobalStatsResult();
        try {
            String cleanHex = hexResponse.startsWith("0x") ? hexResponse.substring(2) : hexResponse;
            if (cleanHex.isEmpty()) {
                return result;
            }
            byte[] data = Numeric.hexStringToByteArray(cleanHex);
            
            result.totalUsers = Numeric.toBigInt(data, 0, 32).intValue();
            result.totalGoldMedals = Numeric.toBigInt(data, 32, 32).intValue();
            result.totalSilverMedals = Numeric.toBigInt(data, 64, 32).intValue();
            result.totalBronzeMedals = Numeric.toBigInt(data, 96, 32).intValue();
            result.totalMedals = Numeric.toBigInt(data, 128, 32).intValue();
        } catch (Exception e) {
            Log.e("ABI_DECODE", "解码全局统计失败: " + e.getMessage());
        }
        return result;
    }
    
    // NFT查询解码方法
    public static UserNftsResult decodeGetUserNfts(String hexResponse) {
        UserNftsResult result = new UserNftsResult();
        try {
            String cleanHex = hexResponse.startsWith("0x") ? hexResponse.substring(2) : hexResponse;
            if (cleanHex.isEmpty()) {
                return result;
            }
            byte[] data = Numeric.hexStringToByteArray(cleanHex);
            
            // 解析动态数组偏移量
            int[] arrayOffsets = new int[4];
            for (int i = 0; i < 4; i++) {
                arrayOffsets[i] = Numeric.toBigInt(data, i * 32, 32).intValue();
            }
            
            result.tokenIds = decodeUint256Array(data, arrayOffsets[0]);
            result.names = decodeStringArray(data, arrayOffsets[1]);
            result.descriptions = decodeStringArray(data, arrayOffsets[2]);
            result.imageUrls = decodeStringArray(data, arrayOffsets[3]);
        } catch (Exception e) {
            Log.e("ABI_DECODE", "解码用户NFT失败: " + e.getMessage());
        }
        return result;
    }
    
    public static NftDataResult decodeGetNftData(String hexResponse) {
        NftDataResult result = new NftDataResult();
        try {
            String cleanHex = hexResponse.startsWith("0x") ? hexResponse.substring(2) : hexResponse;
            if (cleanHex.isEmpty()) {
                return result;
            }
            byte[] data = Numeric.hexStringToByteArray(cleanHex);
            
            // 解析动态字符串偏移量
            int[] stringOffsets = new int[4];
            for (int i = 0; i < 4; i++) {
                stringOffsets[i] = Numeric.toBigInt(data, i * 32, 32).intValue();
            }
            
            result.name = decodeStringStrict(data, stringOffsets[0]);
            result.description = decodeStringStrict(data, stringOffsets[1]);
            result.imageUrl = decodeStringStrict(data, stringOffsets[2]);
            result.owner = decodeStringStrict(data, stringOffsets[3]);
        } catch (Exception e) {
            Log.e("ABI_DECODE", "解码NFT数据失败: " + e.getMessage());
        }
        return result;
    }
}