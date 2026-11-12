package com.example.brokerfi.swap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.brokerfi.R;
import com.example.brokerfi.token.TokenItem;
import com.example.brokerfi.token.TokenStore;
import com.example.brokerfi.token.TokenSwapActivity;
import com.example.brokerfi.token.TokenTopBarHelper;
import com.example.brokerfi.token.TokenTxHistoryStore;
import com.example.brokerfi.token.TokenTxRecord;
import com.example.brokerfi.token.TokenWalletHelper;
import com.example.brokerfi.token.wrappedbkc.wrappedBkcContractHelper;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Assets → Trade adapter. The homepage's original Swap screen remains independent. */
public class BrokerSwapActivity extends AppCompatActivity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private BrokerSwapConfig config;
    private BrokerSwapClient client;
    private String original, wallet = "", error = "";
    private SwapAsset from = SwapAsset.BKC, to = SwapAsset.MUSDT;
    private SwapPoolSnapshot pool;
    private BigInteger nativeBalance, payBalance, receiveBalance, allowance, gasReserve;
    private long readAt, validatedAt;
    private boolean loading, confirming;
    private volatile boolean resumed, preparing;
    private volatile int generation;
    private int slippage = 50, readProgress;
    private EditText amount;
    private TextView receive, status, details, paySymbol, receiveSymbol, payBalanceView, receiveBalanceView;
    private Button submit, slipButton, refreshQuote;
    private SwipeRefreshLayout refresh;
    private LiquidityPanelController liquidity;
    private TradeHistory swapHistory;
    private volatile boolean liquidityVisible;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            render();
            main.postDelayed(this, 15000);
        }
    };
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broker_trade);
        TokenTopBarHelper.bind(this);
        ((TextView)findViewById(R.id.token_page_title)).setText(R.string.trade_title);
        findViewById(R.id.token_info_button).setVisibility(View.GONE);
        findViewById(R.id.trade_tabs).setVisibility(View.VISIBLE);
        amount = findViewById(R.id.token_pay_amount); receive = findViewById(R.id.token_receive_amount);
        amount.setSingleLine(true);
        status = findViewById(R.id.broker_swap_status); details = findViewById(R.id.broker_swap_details);
        paySymbol = findViewById(R.id.token_pay_symbol); receiveSymbol = findViewById(R.id.token_receive_symbol);
        payBalanceView = findViewById(R.id.token_pay_balance); receiveBalanceView = findViewById(R.id.token_receive_balance);
        int selectorWidth = (int)(180 * getResources().getDisplayMetrics().density);
        payBalanceView.setMaxWidth(selectorWidth); receiveBalanceView.setMaxWidth(selectorWidth);
        paySymbol.setTextSize(15); receiveSymbol.setTextSize(15);
        submit = findViewById(R.id.token_confirm_button); slipButton = findViewById(R.id.broker_swap_slippage);
        submit.getLayoutParams().height=android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
        submit.setMinHeight(TradeUi.dp(this,52));
        submit.setPadding(submit.getPaddingLeft(),TradeUi.dp(this,12),submit.getPaddingRight(),TradeUi.dp(this,12));
        refresh = findViewById(R.id.token_refresh); findViewById(R.id.broker_swap_panel).setVisibility(View.VISIBLE);
        try {
            config = BrokerSwapConfig.load(this); client = new BrokerSwapClient(config);
            original = wrappedBkcContractHelper.resolveContractAddress(this);
            String preselect = getIntent().getStringExtra(TokenSwapActivity.EXTRA_PAY_CONTRACT);
            if (preselect != null) for (SwapAsset asset : SwapAsset.values())
                if (!contract(asset).isEmpty() && contract(asset).equalsIgnoreCase(preselect)) { from = asset; break; }
            if (getIntent().getBooleanExtra(TokenSwapActivity.EXTRA_UNWRAP, false) && from == SwapAsset.BKC)
                from = SwapAsset.ORIGINAL_WBKC;
            if (from == SwapAsset.ORIGINAL_WBKC || getIntent().getBooleanExtra(TokenSwapActivity.EXTRA_UNWRAP, false))
                to = SwapAsset.BKC;
            else if (from == SwapAsset.MUSDT) to = SwapAsset.BKC;
        } catch (Exception e) { error = "Deployment configuration unavailable. Trading is disabled."; }
        findViewById(R.id.token_pay_token_selector).setOnClickListener(v -> select(true));
        findViewById(R.id.token_receive_token_selector).setOnClickListener(v -> select(false));
        findViewById(R.id.token_swap_direction).setOnClickListener(v -> {
            if (locked()) return;
            SwapAsset old = from; from = to; to = old; changed();
        });
        findViewById(R.id.token_pay_max).setOnClickListener(v -> {
            if (locked() || !fresh() || payBalance == null || gasReserve == null) return;
            BigInteger max = from == SwapAsset.BKC ? payBalance.subtract(requiredGas()).max(BigInteger.ZERO) : payBalance;
            amount.setText(SwapQuoteMath.format(max, from.decimals));
        });
        slipButton.setOnClickListener(v -> {
            if (locked()) return;
            new AlertDialog.Builder(this).setTitle("Slippage tolerance")
                    .setItems(new String[]{"0.5% (default)", "1%", "2%", "5% (high risk)"}, (d, which) -> {
                        slippage = new int[]{50,100,200,500}[which]; render();
                    }).show();
        });
        int[] slipIds={R.id.video_slip_05,R.id.video_slip_1,R.id.video_slip_2,R.id.video_slip_5};
        for(int i=0;i<slipIds.length;i++) {
            final int bps=new int[]{50,100,200,500}[i];
            findViewById(slipIds[i]).setOnClickListener(v -> {if(!locked() && amm()){slippage=bps;render();}});
        }
        findViewById(R.id.broker_swap_history).setVisibility(View.GONE);
        swapHistory=new TradeHistory(this,R.id.trade_swap_history_toggle,R.id.trade_swap_history_rows,false);
        liquidity=new LiquidityPanelController(this,config,() -> confirming || preparing,this::liquidityChanged);
        findViewById(R.id.trade_tab_swap).setOnClickListener(v -> setLiquidity(false));
        findViewById(R.id.trade_tab_liquidity).setOnClickListener(v -> setLiquidity(true));
        refresh.setOnRefreshListener(() -> { if(liquidityVisible)liquidity.fetch(); else fetch(); });
        refreshQuote = findViewById(R.id.trade_refresh_quote);
        refreshQuote.setOnClickListener(v -> fetch(true));
        submit.setOnClickListener(v -> confirm());
        amount.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            public void afterTextChanged(Editable s) { }
        });
        render();
        updateTabs();
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true;
        String current = BrokerSwapConfig.walletId(TokenWalletHelper.getWalletAddress(this));
        if (!current.equalsIgnoreCase(wallet)) {
            wallet = current; generation++; validatedAt = 0; clearRead();
        }
        liquidity.onResume(wallet);
        fetch(); resumePending(); main.removeCallbacks(tick); main.postDelayed(tick, 15000);
    }
    @Override protected void onPause() { resumed = false; generation++; main.removeCallbacks(tick); if(liquidity!=null)liquidity.onPause(); super.onPause(); }
    @Override protected void onDestroy() { resumed=false; generation++; main.removeCallbacks(tick); if(liquidity!=null)liquidity.onDestroy(); super.onDestroy(); }
    private void setLiquidity(boolean value) {
        if(locked())return;
        if(value==liquidityVisible)return;
        generation++; // Stop subsequent reads for the page that is being hidden.
        liquidityVisible=value;
        findViewById(R.id.trade_swap_content).setVisibility(value?View.GONE:View.VISIBLE);
        findViewById(R.id.trade_liquidity_content).setVisibility(value?View.VISIBLE:View.GONE);
        liquidity.setVisible(value); updateTabs(); if(!value)fetch();
    }
    private void updateTabs() {
        findViewById(R.id.trade_tab_swap).setSelected(!liquidityVisible);
        findViewById(R.id.trade_tab_liquidity).setSelected(liquidityVisible);
        findViewById(R.id.trade_tab_swap).setEnabled(!locked());
        findViewById(R.id.trade_tab_liquidity).setEnabled(!locked());
        ((TextView)findViewById(R.id.trade_tab_swap)).setTypeface(null,!liquidityVisible?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
        ((TextView)findViewById(R.id.trade_tab_liquidity)).setTypeface(null,liquidityVisible?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
    }
    private void liquidityChanged() {
        if(isDestroyed())return;
        if(liquidityVisible)refresh.setRefreshing(liquidity.loading());
        render(); updateTabs();
    }
    private String contract(SwapAsset asset) { return config.tokenAddress(asset, original); }
    private boolean amm() { return SwapAsset.isAmm(from, to); }
    private boolean supported() { return amm() || SwapAsset.isWrap(from, to) || SwapAsset.isUnwrap(from, to); }
    private boolean locked() { return confirming || preparing || liquidity!=null && liquidity.busy() || SwapOperationRunner.active(wallet); }
    private boolean sameAccount(String address) { return address.equalsIgnoreCase(BrokerSwapConfig.walletId(TokenWalletHelper.getWalletAddress(this))); }
    private boolean fresh() { return error.isEmpty() && readAt > 0 && SystemClock.elapsedRealtime() - readAt <= SwapPoolSnapshot.MAX_AGE_MS; }
    private void clearRead() { pool = null; readAt = 0; nativeBalance = payBalance = receiveBalance = allowance = gasReserve = null; }
    private void changed() { generation++; clearRead(); amount.setText(""); render(); fetch(); }
    private void select(boolean paying) {
        if (locked() || config == null) return;
        List<SwapAsset> options = new ArrayList<>();
        for (SwapAsset asset : SwapAsset.values()) {
            SwapAsset other = paying ? to : from;
            if (paying ? SwapAsset.isAmm(asset, other) || SwapAsset.isWrap(asset, other) || SwapAsset.isUnwrap(asset, other)
                    : SwapAsset.isAmm(other, asset) || SwapAsset.isWrap(other, asset) || SwapAsset.isUnwrap(other, asset)) options.add(asset);
        }
        // A pay-side selection can also move to a supported default receive asset.
        if (paying) { options.clear(); java.util.Collections.addAll(options, SwapAsset.values()); }
        String[] labels = new String[options.size()];
        for (int i=0;i<labels.length;i++) labels[i] = options.get(i).symbol + (options.get(i)==SwapAsset.MUSDT ? " (test asset)" : "");
        new AlertDialog.Builder(this).setTitle(paying ? "You pay" : "You receive").setItems(labels, (d, which) -> {
            if (locked()) return;
            if (paying) {
                from = options.get(which);
                if (!supported()) to = from == SwapAsset.ORIGINAL_WBKC || from == SwapAsset.MUSDT ? SwapAsset.BKC : SwapAsset.MUSDT;
            } else to = options.get(which);
            changed();
        }).show();
    }
    private void fetch() { fetch(false); }
    private void fetch(boolean userRequested) {
        if(!resumed || liquidityVisible)return;
        if (config == null || wallet.isEmpty() || locked()) { refresh.setRefreshing(false); render(); return; }
        if (loading) return;
        loading = true; readProgress=0; error = ""; render();
        final int id = generation; final String address = wallet;
        final SwapAsset pay = from, out = to;
        final boolean needsProtocol = pay == SwapAsset.SWAP_WBKC || out == SwapAsset.SWAP_WBKC || pay == SwapAsset.MUSDT || out == SwapAsset.MUSDT;
        final boolean validate = needsProtocol && (validatedAt == 0 || SystemClock.elapsedRealtime()-validatedAt > 600000);
        TradeReadSession session=new TradeReadSession(
                () -> resumed && !liquidityVisible && !locked() && id==generation && sameAccount(address),
                number -> main.post(() -> {
                    if(resumed && !liquidityVisible && id==generation && sameAccount(address)) {
                        readProgress=number; render();
                    }
                }));
        BrokerSwapClient readClient=new BrokerSwapClient(config,session);
        Runnable work = () -> {
            String stage = "Account";
            try {
                session.check();
                String key = TokenWalletHelper.getCurrentPrivateKey(getApplicationContext());
                if (!sameAccount(address)) throw new IllegalStateException("Account changed");
                if (validate) {
                    stage = "Contract validation";
                    readClient.validate(key);
                    final long checkedAt = SystemClock.elapsedRealtime();
                    main.post(() -> {
                        // Metadata/code validation succeeded independently of later volatile reads.
                        // A failed reserve/balance read must not restart all eight immutable checks.
                        if (!isDestroyed() && resumed && !liquidityVisible && id == generation && sameAccount(address)) {
                            validatedAt = checkedAt;
                            importProtocolTokens();
                        }
                    });
                }
                stage = "BKC balance";
                BigInteger nativeAmount = readClient.balance(key, address, "");
                stage = "Payment asset balance";
                BigInteger payAmount = pay == SwapAsset.BKC ? nativeAmount : readClient.balance(key, address, contract(pay));
                stage = "Receiving asset balance";
                BigInteger receiveAmount = out == SwapAsset.BKC ? nativeAmount : readClient.balance(key, address, contract(out));
                stage = "Network fee";
                BigInteger gas = readClient.gasReserve();
                stage = "Allowance";
                BigInteger approved = SwapAsset.isAmm(pay, out) && pay != SwapAsset.BKC
                        ? readClient.allowance(key, address, contract(pay)) : BigInteger.ZERO;
                // Pool calls on Dash may wait for execution. Capture the quote LAST so balance/allowance
                // reads cannot consume its 45-second validity before it reaches the screen.
                stage = "Pool";
                SwapPoolSnapshot snapshot = SwapAsset.isAmm(pay, out) ? readClient.pool(key) : null;
                main.post(() -> {
                    loading = false; if(!liquidityVisible)refresh.setRefreshing(false);
                    if (isDestroyed()) return;
                    if (!resumed || liquidityVisible || id != generation || !sameAccount(address)) { fetch(); return; }
                    pool = snapshot; nativeBalance = nativeAmount; payBalance = payAmount; receiveBalance = receiveAmount;
                    gasReserve = gas; allowance = approved;
                    readAt = snapshot == null ? SystemClock.elapsedRealtime() : snapshot.capturedAtMs; error = "";
                    render();
                });
            } catch (Exception e) {
                final String failedStage = stage, reason=TradeReadFailure.describe(e);
                final boolean cancelled=e instanceof TradeReadSession.Cancelled;
                main.post(() -> {
                    loading = false; if(!liquidityVisible)refresh.setRefreshing(false);
                    if (isDestroyed()) return;
                    if(cancelled || !resumed || liquidityVisible || id!=generation || !sameAccount(address)) { fetch(); return; }
                    readAt = 0;
                    error = failedStage + "："+reason+". Cached data is for reference only. Refresh the quote. This read did not send a transaction."; render();
                });
            }
        };
        if (userRequested) TradeReadQueue.executeUser(work); else TradeReadQueue.execute(work);
    }
    private void importProtocolTokens() {
        for (SwapAsset asset : new SwapAsset[]{SwapAsset.SWAP_WBKC, SwapAsset.MUSDT}) {
            if (TokenStore.contains(this, contract(asset))) continue;
            TokenItem item = new TokenItem(); item.setContractAddress(contract(asset)); item.setSymbol(asset.symbol);
            item.setName(asset==SwapAsset.MUSDT ? "BrokerSwap mock USDT (test only)" : "BrokerSwap wrapped BKC");
            item.setDecimals(asset.decimals); item.setEnabled(true); item.setBuiltIn(false); TokenStore.addCustom(this,item);
        }
    }
    private boolean needsApproval(BigInteger input) { return amm() && from != SwapAsset.BKC && (allowance == null || allowance.compareTo(input)<0); }
    private String loadingText() {
        return "Loading quote...";
    }
    private BigInteger requiredGas() {
        if (gasReserve == null) return BigInteger.ZERO;
        return amm() && from != SwapAsset.BKC ? gasReserve.multiply(BigInteger.valueOf(2)) : gasReserve;
    }
    private TokenTxRecord unresolved() {
        for (TokenTxRecord r : TokenTxHistoryStore.getAll(this, wallet)) if (TokenTxHistoryStore.isBlockingSwap(r)) return r;
        return null;
    }
    private TokenTxRecord deferred() {
        for(TokenTxRecord r:TokenTxHistoryStore.getAll(this,wallet))if(TradeRecoveryPolicy.deferred(r))return r;
        return null;
    }
    private SwapQuoteMath.Quote quote(BigInteger input) { return SwapQuoteMath.quote(pool, from, to, input, slippage, SystemClock.elapsedRealtime()); }
    private String validation(BigInteger input, SwapQuoteMath.Quote quote) {
        if (!sameAccount(wallet)) return "Account changed. Reopen this page.";
        if (!fresh() || (amm() && (pool==null || !pool.isFresh(SystemClock.elapsedRealtime())))) return "Quote expired. Pull down to refresh.";
        if (!supported()) return "This asset pair is not supported.";
        if (payBalance==null || payBalance.compareTo(input)<0) return "Insufficient payment asset balance.";
        if (nativeBalance==null || nativeBalance.compareTo(requiredGas().add(from==SwapAsset.BKC ? input : BigInteger.ZERO))<0)
            return "Insufficient BKC for the amount and reserved network fee.";
        if (quote!=null && quote.blocked()) return "Price impact is 10% or higher. Trading is blocked.";
        if (unresolved()!=null) return "An unresolved transaction exists. Check Swap History first.";
        return "";
    }
    private void render() {
        if (submit == null) return;
        paySymbol.setText(from.symbol); receiveSymbol.setText(to.symbol);
        ((TextView)findViewById(R.id.token_pay_token_icon)).setText(from==SwapAsset.MUSDT ? "M" : "B");
        ((TextView)findViewById(R.id.token_receive_token_icon)).setText(to==SwapAsset.MUSDT ? "M" : "B");
        payBalanceView.setText("Balance: " + TradeDisplay.amount(payBalance,from.decimals)+" "+from.symbol + (payBalance == null ? "" : fresh()?"":" (stale)"));
        receiveBalanceView.setText("Balance: " + TradeDisplay.amount(receiveBalance,to.decimals)+" "+to.symbol + (receiveBalance == null ? "" : fresh()?"":" (stale)"));
        slipButton.setText("Slippage: " + slippage/100.0 + "% ▾"); slipButton.setEnabled(!locked() && amm());
        int[] slipIds={R.id.video_slip_05,R.id.video_slip_1,R.id.video_slip_2,R.id.video_slip_5};
        for(int i=0;i<slipIds.length;i++) {
            TextView choice=findViewById(slipIds[i]);boolean selected=slippage==new int[]{50,100,200,500}[i];
            choice.setSelected(selected);choice.setEnabled(!locked() && amm());
            choice.setTypeface(null,selected?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
        }
        amount.setEnabled(!locked());
        TokenTxRecord pending=unresolved();
        String operation = TradeStatusText.operation(confirming, preparing, SwapOperationRunner.active(wallet), pending);
        String statusText=(wallet.isEmpty() ? "Please sign in to your wallet." : !operation.isEmpty() ? operation
                : error)
                +(pending!=null ? loading?"\n"+loadingText():!error.isEmpty()?"\n"+error:"":"")
                +(deferred()!=null?"\nPrevious outcome is still unverified. Continuation risk accepted. Tap to review.":"");
        TradeUi.status(status,statusText,pending!=null || !error.isEmpty() || deferred()!=null);
        status.setOnClickListener(v -> { TokenTxRecord r=unresolved(); if(r==null)r=deferred(); if(r!=null)TradeHistory.showRecord(this,config,wallet,r,() -> { render(); fetch(); }); });
        if(swapHistory!=null)swapHistory.bind(config,wallet,() -> { render(); fetch(); });
        updateTabs();
        String action=amm()?"Swap":from==SwapAsset.BKC?"Wrap":"Unwrap";
        boolean quoteFresh = fresh();
        refreshQuote.setVisibility(quoteFresh || loading ? View.GONE : View.VISIBLE);
        refreshQuote.setEnabled(!loading && !locked() && pending == null && config != null && !wallet.isEmpty());
        String inputHint="";
        TextView feedback=findViewById(R.id.trade_swap_feedback);
        TextView rate=findViewById(R.id.video_rate);
        rate.setText("—");
        receive.setText("0");receive.setTextColor(0xffCBD2DC); details.setText(""); submit.setText(action); submit.setEnabled(false);
        if (config == null || wallet.isEmpty()) { TradeUi.hint(feedback,"Wallet or deployment is not ready.",true); return; }
        try {
            BigInteger input = SwapQuoteMath.parseUnits(amount.getText().toString(), from.decimals);
            SwapQuoteMath.Quote q = amm() && pool != null ? SwapQuoteMath.reference(pool,from,to,input,slippage) : null;
            BigInteger output = q==null ? amm() ? null : input : q.output;
            receive.setText(TradeDisplay.amount(output,to.decimals));
            receive.setTextColor(output==null?0xffCBD2DC:0xff111827);
            rate.setText(q==null?amm()?"—":"1 "+from.symbol+" = 1 "+to.symbol:TradeDisplay.rate(q).replace(" ≈ "," = ~"));
            String quoteNotice = "";
            if (!quoteFresh) {
                quoteNotice = error.isEmpty() ? "\nQuote expired. Refresh quote to continue."
                        : "\nPrevious quote — for reference only";
            }
            details.setText(q==null?amm()?"":"Min received: "+TradeDisplay.amount(input,to.decimals)+" "+to.symbol
                    :"Min received: "+TradeDisplay.amount(q.minimum,q.to.decimals)+" "+q.to.symbol
                        +(q.impactBps>=200?"\nPrice impact: "+q.impactBps/100.0+"% (high risk)":"")
                        +quoteNotice);
            if (!supported()) inputHint="This asset pair is not supported.";
            else if (quoteFresh) {
                String problem = validation(input,q);
                inputHint=problem;
                submit.setText(problem.isEmpty() && needsApproval(input) ? "Approve "+from.symbol : action);
                submit.setEnabled(problem.isEmpty() && !locked());
            }
        } catch (Exception e) { if(amount.getText().length()>0)inputHint="Invalid decimal precision or amount too small."; }
        if (!operation.isEmpty() || locked()) {
            submit.setText(action); submit.setEnabled(false); inputHint="";
        } else if (loading) {
            submit.setText(loadingText());
            submit.setEnabled(false);
        }
        TradeUi.hint(feedback,inputHint,true);
        TradeButtons.state(submit,submit.getText().toString(),submit.isEnabled(),pending!=null || !error.isEmpty() || !loading && readAt>0 && !fresh());
    }
    private void confirm() {
        if (loading || locked()) return;
        try {
            BigInteger input = SwapQuoteMath.parseUnits(amount.getText().toString(),from.decimals);
            if (!sameAccount(wallet) || unresolved()!=null || !supported()) { toast("Check your account, assets and unresolved transactions."); return; }
            if (!fresh()) {
                render();
                return;
            }
            SwapQuoteMath.Quote q = amm() ? quote(input) : null;
            String problem = validation(input,q); if (!problem.isEmpty()) { toast(problem); return; }
            boolean approval = needsApproval(input);
            final int id = generation; final String address = wallet; final SwapAsset pay = from, out = to;
            confirming = true; render();
            android.content.DialogInterface.OnClickListener onConfirm=(d,w) -> {
                        if (!approval && q!=null && q.impactBps>=500) {
                            main.post(() -> {
                            confirming = true; render();
                            AlertDialog warning = new AlertDialog.Builder(this).setTitle("Price impact exceeds 5%")
                                    .setMessage("This trade will significantly change the pool price and may cause a loss. Continue?")
                                    .setNegativeButton("Cancel",null).setPositiveButton("Continue anyway", (d2,w2) -> prepare(id,address,pay,out,input,q,approval)).create();
                            warning.setOnDismissListener(x -> { confirming=false; render(); }); warning.show();
                            });
                        } else prepare(id,address,pay,out,input,q,approval);
                    };
            String fields="Pay: "+SwapQuoteMath.format(input,pay.decimals)+" "+pay.symbol
                    +"\nMin Receive: "+SwapQuoteMath.format(q==null?input:q.minimum,out.decimals)+" "+out.symbol
                    +"\nRate: "+(q==null?"1 "+pay.symbol+" = 1 "+out.symbol:TradeDisplay.rate(q).replace(" ≈ "," = ~"))
                    +"\nSlippage: "+slippage/100.0+"%\nPrice impact: "+(q==null?0:q.impactBps/100.0)+"%";
            AlertDialog dialog=approval?TradeVideoUi.approval(this,pay.symbol,SwapQuoteMath.format(input,pay.decimals),onConfirm)
                    :TradeVideoUi.confirmation(this,"Confirm swap",fields,onConfirm);
            dialog.setOnDismissListener(d -> { confirming=false; render(); });
            if(approval)TradeVideoUi.showApproval(dialog);else TradeVideoUi.show(dialog);
        } catch (Exception e) { toast("Refresh the quote and check the amount."); }
    }
    private void prepare(int id, String address, SwapAsset pay, SwapAsset out, BigInteger input, SwapQuoteMath.Quote confirmed, boolean approval) {
        preparing = true; render();
        TradeReadQueue.executeUser(() -> {
            String stage = "Account status";
            try {
                if (id!=generation || !sameAccount(address)) throw new IllegalStateException("Account or assets changed. Confirm again.");
                String key = TokenWalletHelper.getCurrentPrivateKey(getApplicationContext());
                stage = "BKC balance";
                BigInteger nativeAmount = client.balance(key,address,"");
                stage = "Payment asset balance";
                BigInteger balance = pay==SwapAsset.BKC ? nativeAmount : client.balance(key,address,contract(pay));
                stage = "Network fee";
                BigInteger reserve = client.gasReserve().multiply(BigInteger.valueOf(confirmed!=null && pay!=SwapAsset.BKC ? 2 : 1));
                if (balance.compareTo(input)<0 || nativeAmount.compareTo(reserve.add(pay==SwapAsset.BKC ? input : BigInteger.ZERO))<0)
                    throw new IllegalStateException("Insufficient balance or network fee.");
                SwapPoolSnapshot latestPool=null;
                if (confirmed!=null) {
                    // Read allowance BEFORE reserves, so a slow signed read cannot age the final quote.
                    stage = "Allowance";
                    if (!approval && pay!=SwapAsset.BKC && client.allowance(key,address,contract(pay)).compareTo(input)<0)
                        throw new IllegalStateException("Insufficient allowance. Approve this amount again.");
                    stage = "Latest pool quote";
                    latestPool=client.pool(key);
                    SwapQuoteMath.Quote current = SwapQuoteMath.quote(latestPool,pay,out,input,confirmed.slippageBps,SystemClock.elapsedRealtime());
                    if (current.output.compareTo(confirmed.minimum)<0 || current.blocked()
                            || current.impactBps>=500 && confirmed.impactBps<500)
                        throw new IllegalStateException("Price changed. Refresh and confirm again.");
                }
                if(latestPool!=null && !latestPool.isFresh(SystemClock.elapsedRealtime()))throw new IllegalStateException("Quote expired");
                stage = "Transaction encoding";
                String target, data; BigInteger value = BigInteger.ZERO;
                long deadline=confirmed!=null && !approval?System.currentTimeMillis()/1000+300:0;
                if (approval) { target=contract(pay); data=BrokerSwapAbi.call("approve",new Address(config.router()),new Uint256(input)); }
                else if (confirmed!=null) { target=config.router(); data=BrokerSwapAbi.swap(config,confirmed,BrokerSwapConfig.walletAddress(address),deadline); if(pay==SwapAsset.BKC)value=input; }
                else { target=contract(pay.isWrapped()?pay:out); data=pay==SwapAsset.BKC ? wrappedBkcContractHelper.encodeDeposit() : wrappedBkcContractHelper.encodeWithdraw(input); if(pay==SwapAsset.BKC)value=input; }
                TokenTxRecord record = record(pay,out,input,confirmed,approval);
                record.deadlineSeconds=deadline;
                final String sendTarget=target,sendData=data; final BigInteger sendValue=value;
                final SwapPoolSnapshot finalPool=latestPool;
                main.post(() -> {
                    preparing=false;
                    if (isDestroyed() || id!=generation || !sameAccount(address)) { render(); return; }
                    if(finalPool!=null && !finalPool.isFresh(SystemClock.elapsedRealtime())) { readAt=0; error="Checks expired. No transaction was sent. Refresh the quote and confirm again."; render(); return; }
                    if (!SwapOperationRunner.submit(getApplicationContext(),config,address,record,sendTarget,sendData,sendValue,this::completed))
                        toast("A transaction is already in progress.");
                    render();
                });
            } catch (Exception e) {
                final String failedStage = stage;
                main.post(() -> {
                    preparing=false;
                    if(!isDestroyed()) {
                        readAt=0; error="Pre-send "+failedStage+" check failed. No transaction was sent. Refresh the quote and confirm again.";
                        toast(error); render();
                    }
                });
            }
        });
    }
    private TokenTxRecord record(SwapAsset pay, SwapAsset out, BigInteger input, SwapQuoteMath.Quote q, boolean approval) {
        TokenTxRecord r = new TokenTxRecord(); r.operationId=UUID.randomUUID().toString(); r.timestampMs=System.currentTimeMillis();
        r.type=approval?"APPROVAL":TokenTxRecord.TYPE_SWAP; r.fromAsset=pay.name(); r.toAsset=out.name();
        r.fromSymbol=pay.symbol; r.toSymbol=out.symbol; r.inputUnits=input.toString(); r.amountDisplay=SwapQuoteMath.format(input,pay.decimals);
        r.contractAddress=contract(pay); r.toContractAddress=approval?null:contract(out); r.routerAddress=config.router(); r.chainId=config.chainId();
        if(q!=null) { r.estimatedOutput=SwapQuoteMath.format(q.output,out.decimals); r.minimumOutput=SwapQuoteMath.format(q.minimum,out.decimals);
            r.slippageBps=q.slippageBps; r.impactBps=q.impactBps; }
        else { r.estimatedOutput=SwapQuoteMath.format(input,out.decimals); r.minimumOutput=r.estimatedOutput; }
        return r;
    }
    private void completed(TokenTxRecord record) {
        if (isDestroyed()) return;
        toast("APPROVAL".equals(record.type) && "SUCCESS".equals(record.status)
                ? "Approval successful" : "SUCCESS".equals(record.status)?"Swap successful":VideoTradePresentation.status(record.status));
        readAt=0; fetch(); render();
        liquidity.changed();
        if ("SUCCESS".equals(record.status) && !"APPROVAL".equals(record.type)) amount.setText("");
    }
    private void resumePending() {
        if (config==null || wallet.isEmpty()) return;
        TokenTxRecord pending=unresolved();
        if (pending!=null && SwapReceipt.validHash(pending.txHash))
            SwapOperationRunner.resume(getApplicationContext(),config,wallet,pending,this::completed);
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
}
