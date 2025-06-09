package com.example.brokerfi.xc.model;

import java.math.BigInteger;
import java.util.Objects;

public class NFT {
    private final String imageBase64;
    private final BigInteger NFTId;

    private final String accountNumber;

    private final String name;
    private final BigInteger shares;
    private final BigInteger price;
    private final boolean isListed;
    private final BigInteger listingId;

    public NFT(BigInteger nftId, String accountNumber, String imageBase64, String name,
               BigInteger shares, BigInteger price, boolean isListed, BigInteger listingId) {
        this.NFTId = nftId;
        this.accountNumber = accountNumber;
        this.imageBase64 = imageBase64;
        this.name = name;
        this.shares = shares;
        this.isListed = isListed;
        this.listingId = listingId;

        if (!isListed) {
            this.price = BigInteger.ZERO;
        } else {
            this.price = price;
        }
    }


    public BigInteger getId() { return NFTId; }

    public String getAccountNumber() { return accountNumber; }

    public String getImageBase64() { return imageBase64; }

    public String getName() { return name; }
    public BigInteger getPrice() { return price; }
    public BigInteger getShares() { return shares; }
    public boolean isListed() { return isListed; }
    public BigInteger getListingId(){ return listingId;}


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NFT nft = (NFT) o;
        return isListed == nft.isListed &&
               Objects.equals(price, nft.price) &&
               NFTId == nft.NFTId &&
               Objects.equals(accountNumber, nft.accountNumber) &&
               Objects.equals(shares, nft.shares);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, accountNumber, isListed, price, shares);
    }
}
