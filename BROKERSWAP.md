# Assets and BrokerSwap integration

## Scope

Assets extends the existing token screen with native BKC and a Trade entry. Trade reproduces the reference demo with Swap and Provide Liquidity tabs, confirmation dialogs, and collapsible transaction history. The original homepage Swap entry remains unchanged.

The integration reuses wallet accounts, signing, network configuration, token management, and per-wallet transaction history. It does not deploy contracts or change the server. Arbitrary imported tokens are not automatically tradable.

## Deployment and supported routes

Deployment addresses and code fingerprints are pinned in `app/src/main/assets/broker_swap_deployment.json` on chain 1051. The client validates the network, routers, pool tokens, and decimals before protocol operations.

- BKC / mUSDT swaps use the configured pool and wrap or unwrap native BKC through the router.
- Protocol wBKC / mUSDT swaps use the same pool.
- Native BKC can be wrapped or unwrapped with the configured wBKC contracts.
- The pre-existing wallet wBKC and protocol wBKC are separate contracts. Wallet wBKC must be unwrapped before exchanging it through the protocol pool.
- mUSDT is a test token, not real USDT.

## Responsibilities

- `swap`: ABI encoding, integer quote math, deployment validation, chain communication, operation execution, UI controllers, and transaction presentation.
- `token`: asset selection, native balances and history, and shared transaction storage.
- `res`: layouts, styles, icons, and strings.
- `src/test`: quote, precision, receipt, recovery, and presentation tests.
- `src/androidTest` and `src/debug`: isolated layout tests and a non-exported visual fixture. The fixture does not read accounts or send transactions.

Quotes use on-chain reserves and integer amounts. Display rounding does not change transaction precision. ERC-20 approvals are separate user-confirmed transactions. Deposits and withdrawals use the deployed atomic liquidity router.

Pending and unknown operations are persisted per wallet. Reads may be retried once; transaction submissions are never automatically retried. Receipt recovery queries existing operations without resending them. Private keys and signed requests are not logged.

## Build and validation

Use JDK 17 and configure the Android SDK in an untracked `local.properties`. Keep build output, local SDK paths, signing keys, and IDE machine settings out of commits. The path-check override permits Windows workspace paths containing non-ASCII characters.

```text
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

On Windows, use an ASCII-only project path (or a directory junction) and UTF-8 for Java when running unit tests; bypassing the Android path check alone does not resolve test-worker class loading under a non-ASCII path.

The debug-only layout fixture can be tested on an emulator without funded accounts or live chain transactions. Live financial acceptance is separate from these checks and requires sufficient test balances.

## Current limitations

Dash signed queries can take several minutes. Refreshing a quote does not submit a transaction; stale quotes require a refresh before execution. The current ERC-20 asset page may display zero when a balance query fails, so zero on that page alone does not establish an on-chain balance. Only the configured pool and routes are integrated. Additional wBKC routes have not all received funded live acceptance. Display and historical-record edge cases remain follow-up work.

## Reference

Protocol source and deployment materials: https://github.com/HardyDu/brokerchain-swap
