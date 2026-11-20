package com.example.brokerfi.swap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.brokerfi.R;
import com.example.brokerfi.token.TokenTxHistoryStore;
import com.example.brokerfi.token.TokenTxRecord;
import com.example.brokerfi.token.TokenWalletHelper;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.UUID;

/** Wallet UI adapter for Du's existing atomic LP router. Does not deploy or alter contracts. */
final class LiquidityPanelController {
    private final AppCompatActivity activity;
    private final BrokerSwapConfig config;
    private final BrokerSwapClient client;
    private final TradeQuoteCache quoteCache;
    private final SwipeRefreshLayout refresh;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final EditText bkc,musdt;
    private final TextView status,balanceBkc,balanceMusdt,addQuote,owned,position,fees,removeQuote,percentLabel;
    private final Button approveAdd,add,approveRemove,remove,refreshQuote;
    private final SeekBar slider;
    private final TradeHistory history;
    private String wallet="",error="";
    private Snapshot snapshot;
    private TradeQuoteCache.LiquidityQuote quote;
    private TradeReadSession readSession;
    private static final long REFRESH_TAP_GUARD_MS = 650L;
    private long validatedAt, quoteActionsEnabledAt;
    private volatile int generation;
    private int percent;
    private volatile boolean resumed,preparing;
    private boolean loading,confirming,updating,lastEditedBkc=true;
    private final Runnable tick=new Runnable() { public void run() {
        if(!resumed) return;
        render();
        main.postDelayed(this,15000);
    }};
    private static final class Snapshot {
        final SwapPoolSnapshot pool;
        final BigInteger supply,nativeBalance,musdtBalance,lpBalance,musdtAllowance,lpAllowance,gas;
        Snapshot(SwapPoolSnapshot pool,BigInteger supply,BigInteger n,BigInteger m,BigInteger lp,BigInteger ma,BigInteger la,BigInteger gas) {
            if(supply.signum()<=0 || lp.compareTo(supply)>0)throw new IllegalArgumentException("Invalid LP supply");
            this.pool=pool; this.supply=supply; nativeBalance=n; musdtBalance=m; lpBalance=lp; musdtAllowance=ma; lpAllowance=la; this.gas=gas;
        }
        boolean fresh() { return pool.isFresh(SystemClock.elapsedRealtime()); }
    }
    LiquidityPanelController(AppCompatActivity activity, BrokerSwapConfig config) {
        this.activity = activity;
        this.config = config;
        refresh = activity.findViewById(R.id.token_refresh);
        refresh.setOnRefreshListener(this::fetch);
        client=config==null?null:new BrokerSwapClient(config);
        quoteCache=config==null || !config.hasLiquidity()?null:new TradeQuoteCache(activity,config);
        if(quoteCache!=null)quote=quoteCache.loadLiquidity();
        bkc=activity.findViewById(R.id.lp_bkc_amount); musdt=activity.findViewById(R.id.lp_musdt_amount);
        status=activity.findViewById(R.id.lp_status); balanceBkc=activity.findViewById(R.id.lp_bkc_balance);
        balanceMusdt=activity.findViewById(R.id.lp_musdt_balance); addQuote=activity.findViewById(R.id.lp_add_quote);
        owned=activity.findViewById(R.id.lp_owned); position=activity.findViewById(R.id.lp_position); fees=activity.findViewById(R.id.lp_fees);
        removeQuote=activity.findViewById(R.id.lp_remove_quote); percentLabel=activity.findViewById(R.id.lp_remove_percent);
        approveAdd=activity.findViewById(R.id.lp_approve_add); add=activity.findViewById(R.id.lp_add);
        approveRemove=activity.findViewById(R.id.lp_approve_remove); remove=activity.findViewById(R.id.lp_remove);
        slider=activity.findViewById(R.id.lp_remove_slider);
        refreshQuote=activity.findViewById(R.id.lp_refresh_quote);
        refreshQuote.setOnClickListener(v -> fetch(true));
        history=new TradeHistory(activity,R.id.lp_history_toggle,R.id.lp_history_rows,true);
        bkc.addTextChangedListener(watcher(true)); musdt.addTextChangedListener(watcher(false));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar,int value,boolean user) { percent=value; render(); }
            public void onStartTrackingTouch(SeekBar bar) {} public void onStopTrackingTouch(SeekBar bar) {}
        });
        int[] ids={R.id.lp_percent_25,R.id.lp_percent_50,R.id.lp_percent_75,R.id.lp_percent_100};
        for(int i=0;i<ids.length;i++) { final int value=(i+1)*25; activity.findViewById(ids[i]).setOnClickListener(v -> { if(!locked())slider.setProgress(value); }); }
        approveAdd.setOnClickListener(v -> confirm(true,true)); add.setOnClickListener(v -> confirm(true,false));
        approveRemove.setOnClickListener(v -> confirm(false,true)); remove.setOnClickListener(v -> confirm(false,false));
        status.setOnClickListener(v -> { TokenTxRecord pending=unresolved(); if(pending==null)pending=deferred(); if(pending!=null)TradeHistory.showRecord(activity,config,wallet,pending,this::afterHistory); });
        activity.findViewById(R.id.lp_position_help).setOnClickListener(v -> TradeUi.help(activity,"Position and fee estimates",
                "Position value uses the current pool price.\nEstimated fees reflect pool growth per LP token observed on this device while the LP balance stays unchanged. They are not realized profit.\nLP transfers reset the observation baseline. Profit or loss is unavailable without reliable cost records.\nFees stay in the pool and cannot be claimed separately."));
        fees.setContentDescription("Estimated pool growth since this device's observation baseline; not realized profit or a separately claimable balance.");
        activity.findViewById(R.id.lp_position_change).setContentDescription("Position Change: unavailable without reliable cost records");
        render();
    }
    private TextWatcher watcher(boolean fromBkc) { return new TextWatcher() {
        public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
        public void onTextChanged(CharSequence s,int start,int before,int count) {
            if(updating)return; lastEditedBkc=fromBkc; matchOther(); render();
        }
        public void afterTextChanged(Editable text) {}
    }; }
    private void matchOther() {
        if(quote==null || locked())return;
        EditText input=lastEditedBkc?bkc:musdt, output=lastEditedBkc?musdt:bkc;
        try {
            BigInteger units=SwapQuoteMath.parseUnits(input.getText().toString(),lastEditedBkc?18:6);
            BigInteger other=LiquidityMath.counterpart(quote.pool,units,lastEditedBkc);
            updating=true; output.setText(SwapQuoteMath.format(other,lastEditedBkc?6:18));
        } catch(Exception ignored) { updating=true; output.setText(""); }
        finally { updating=false; }
    }
    private boolean locked() { return confirming || preparing || SwapOperationRunner.active(wallet); }
    void onResume(String account) {
        if(!wallet.equalsIgnoreCase(account)) {
            wallet=account; generation++; validatedAt=0; snapshot=null; error="";
            updating=true; bkc.setText(""); musdt.setText(""); updating=false; slider.setProgress(0);
        }
        resumed=true; main.removeCallbacks(tick); main.postDelayed(tick,15000);
        resumePending(); fetch(); render();
    }
    void onPause() {
        resumed=false; generation++;
        if(readSession!=null)readSession.cancel();
        main.removeCallbacks(tick);
    }
    void onDestroy() { resumed=false; generation++; main.removeCallbacks(tick); }
    private boolean sameAccount(String address) { return address.equalsIgnoreCase(BrokerSwapConfig.walletId(TokenWalletHelper.getWalletAddress(activity))); }
    private TokenTxRecord unresolved() {
        for(TokenTxRecord record:TokenTxHistoryStore.getAll(activity,wallet))if(TokenTxHistoryStore.isBlockingSwap(record))return record;
        return null;
    }
    private TokenTxRecord deferred() {
        for(TokenTxRecord r:TokenTxHistoryStore.getAll(activity,wallet))if(TradeRecoveryPolicy.deferred(r))return r;
        return null;
    }
    private void fetch() { fetch(false); }
    private void fetch(boolean userRequested) {
        if (!resumed || loading || locked() || config == null || !config.hasLiquidity() || wallet.isEmpty()) {
            if (!loading) refresh.setRefreshing(false);
            return;
        }
        loading=true; error=""; render();
        final int id=generation; final String account=wallet;
        final boolean validate=validatedAt==0 || SystemClock.elapsedRealtime()-validatedAt>600000;
        TradeReadSession session=new TradeReadSession(
                () -> resumed && !preparing && !SwapOperationRunner.active(account) && id==generation && sameAccount(account), null);
        readSession=session;
        BrokerSwapClient readClient=new BrokerSwapClient(config,session);
        Runnable work = () -> {
            String stage="Account";
            try {
                session.check();
                if(!sameAccount(account))throw new IllegalStateException("Account changed");
                String key=TokenWalletHelper.getCurrentPrivateKey(activity.getApplicationContext());
                if(validate) {
                    stage="Liquidity contract validation"; readClient.validateLiquidity(key);
                    long checked=SystemClock.elapsedRealtime();
                    main.post(() -> { if(!activity.isDestroyed() && resumed && id==generation && sameAccount(account))validatedAt=checked; });
                }
                stage="Balances, LP and allowances";
                Snapshot read=readSnapshot(readClient,key,account);
                main.post(() -> {
                    readSession=null; loading=false; refresh.setRefreshing(false);
                    if(activity.isDestroyed())return;
                    if(!resumed || id!=generation || !sameAccount(account) || preparing) { fetch(); return; }
                    if (userRequested) {
                        quoteActionsEnabledAt = SystemClock.elapsedRealtime() + REFRESH_TAP_GUARD_MS;
                        main.postDelayed(() -> {
                            if (resumed && !activity.isDestroyed()) render();
                        }, REFRESH_TAP_GUARD_MS);
                    }
                    snapshot=read;
                    quote=new TradeQuoteCache.LiquidityQuote(read.pool,read.supply);
                    quoteCache.saveLiquidity(quote);
                    error=""; matchOther(); render();
                });
            } catch(Exception e) {
                final String failed=stage, reason=TradeReadFailure.describe(e);
                final boolean cancelled=e instanceof TradeReadSession.Cancelled;
                main.post(() -> {
                    readSession=null; loading=false; refresh.setRefreshing(false);
                    if(activity.isDestroyed())return;
                    if(cancelled || !resumed || id!=generation || !sameAccount(account) || preparing) { fetch(); return; }
                    error=failed+": "+reason+". Refresh the quote to try again."; render();
                });
            }
        };
        if(userRequested)TradeReadQueue.executeUser(work);else TradeReadQueue.execute(work);
    }
    private Snapshot readSnapshot(String key,String address) throws Exception {
        return readSnapshot(client,key,address);
    }
    private Snapshot readSnapshot(BrokerSwapClient client,String key,String address) throws Exception {
        BigInteger n=client.balance(key,address,""), m=client.balance(key,address,config.musdt());
        BigInteger lp=client.balance(key,address,config.pair()), supply=client.totalSupply(key);
        BigInteger ma=client.allowance(key,address,config.musdt(),config.liquidityRouter());
        BigInteger la=client.allowance(key,address,config.pair(),config.liquidityRouter()), gas=client.gasReserve();
        // Reserve quote LAST: signed gateway reads can be slow. Every action still does fresh preflight.
        return new Snapshot(client.pool(key),supply,n,m,lp,ma,la,gas);
    }
    private String baseProblem() {
        if(config==null || !config.hasLiquidity())return "Liquidity deployment is not ready.";
        if(wallet.isEmpty())return "Please sign in to your wallet.";
        if(!sameAccount(wallet))return "Account changed. Reopen this page.";
        if(unresolved()!=null)return "An unresolved transaction exists. Tap the message above to review.";
        return "";
    }
    private String addProblem(Snapshot s,BigInteger desiredM,BigInteger desiredB) {
        String problem=baseProblem(); if(!problem.isEmpty() || s==null)return problem;
        if(s.musdtBalance.compareTo(desiredM)<0)return "Insufficient mUSDT balance.";
        BigInteger reserve=LiquiditySafety.reserve(s.gas,s.musdtAllowance.compareTo(desiredM)<0);
        if(s.nativeBalance.compareTo(desiredB.add(reserve))<0)return "Insufficient BKC for the amount and network fee.";
        return "";
    }
    private String removeProblem(Snapshot s,BigInteger lp) {
        String problem=baseProblem(); if(!problem.isEmpty())return problem;
        if(s==null)return "Refresh to load your LP balance.";
        if(lp.signum()<=0)return "Select a percentage to remove.";
        if(s.lpBalance.compareTo(lp)<0)return "Insufficient LP balance.";
        BigInteger reserve=LiquiditySafety.reserve(s.gas,s.lpAllowance.compareTo(lp)<0);
        if(s.nativeBalance.compareTo(reserve)<0)return "Insufficient BKC for the network fee.";
        return "";
    }
    private void render() {
        Snapshot s=snapshot; boolean lock=locked(); TokenTxRecord pending=unresolved();
        String operation=TradeStatusText.operation(confirming,preparing,SwapOperationRunner.active(wallet),pending);
        String state=!operation.isEmpty()?operation
                : lock?"Checking another operation…":!error.isEmpty()?error:"";
        TradeUi.status(status,state+(pending!=null?loading?"\n"+loadingText():!error.isEmpty()?"\n"+error:"":"")
                +(deferred()!=null?"\nPrevious outcome is still unverified. Continuation risk accepted. Tap to review.":""),pending!=null || !error.isEmpty() || deferred()!=null);
        bkc.setEnabled(!lock); musdt.setEnabled(!lock); slider.setEnabled(!lock);
        for(int id:new int[]{R.id.lp_percent_25,R.id.lp_percent_50,R.id.lp_percent_75,R.id.lp_percent_100})activity.findViewById(id).setEnabled(!lock);
        balanceBkc.setText("Balance: "+(s==null?"—":TradeDisplay.amount(s.nativeBalance,18))+" BKC");
        balanceMusdt.setText("Balance: "+(s==null?"—":TradeDisplay.amount(s.musdtBalance,6))+" mUSDT");
        owned.setText("Your LP tokens: "+(s==null?"—":TradeDisplay.amount(s.lpBalance,18))+" LPC");
        percentLabel.setText("Remove "+percent+"% of your LP tokens");
        String base=baseProblem();
        refreshQuote.setText(loading ? R.string.trade_quote_refreshing : R.string.trade_refresh_quote);
        refreshQuote.setEnabled(!loading && !lock && base.isEmpty());
        boolean actionReady = SystemClock.elapsedRealtime() >= quoteActionsEnabledAt;
        boolean blocked=pending!=null;
        approveAdd.setVisibility(View.GONE);add.setVisibility(View.VISIBLE);
        TradeButtons.state(approveAdd,"Approve mUSDT",false,blocked); TradeButtons.state(add,"Enter one amount to preview the pool ratio",false,blocked);
        String poolRate=quote==null?"":TradeDisplay.poolRate(quote.pool,SwapAsset.BKC,SwapAsset.MUSDT)
                +"\n"+TradeDisplay.quoteUpdated(activity,quote.pool)+"\n\n";
        addQuote.setText(poolRate+"Enter one amount to preview the pool ratio");
        String addHint="",removeHint="";
        try {
            BigInteger desiredM=SwapQuoteMath.parseUnits(musdt.getText().toString(),6), desiredB=SwapQuoteMath.parseUnits(bkc.getText().toString(),18);
            if(quote!=null) {
                LiquidityMath.AddQuote q=LiquidityMath.add(quote.pool,quote.supply,desiredM,desiredB);
                addQuote.setText(poolRate+"Pool will use "+TradeDisplay.amount(q.bkc,18)+" BKC + "+TradeDisplay.amount(q.musdt,6)+" mUSDT"
                        +"\nEstimated LP tokens: "+TradeDisplay.amount(q.lp,18)+" LPC\nEstimated pool share: "+share(q.lp,quote.supply.add(q.lp))+"%");
                String problem=addProblem(s,desiredM,desiredB); boolean approved=s==null || s.musdtAllowance.compareTo(desiredM)>=0;
                boolean valid=problem.isEmpty() && !lock && actionReady;
                approveAdd.setVisibility(approved?View.GONE:View.VISIBLE);add.setVisibility(approved?View.VISIBLE:View.GONE);
                TradeButtons.state(approveAdd,"Approve mUSDT",valid && !approved,blocked || !problem.isEmpty());
                TradeButtons.state(add,"Add Liquidity",valid && approved,blocked || !problem.isEmpty());
                if(!problem.equals(base))addHint=problem;
            }
        } catch(Exception ignored) { if(bkc.length()>0 && musdt.length()>0)addHint="Invalid precision or amount too small."; }
        approveRemove.setVisibility(View.GONE);remove.setVisibility(View.VISIBLE);
        TradeButtons.state(approveRemove,"Approve LP",false,blocked); TradeButtons.state(remove,"Remove Liquidity",false,blocked);
        removeQuote.setText("You will receive:\n— BKC + — mUSDT");
        try {
            if(s!=null) {
                BigInteger lp=LiquidityMath.portion(s.lpBalance,percent);
                BigInteger[] outputs=LiquidityMath.remove(s.pool,s.supply,lp);
                if(LiquidityMath.minimum(outputs[0]).signum()==0 || LiquidityMath.minimum(outputs[1]).signum()==0)throw new IllegalArgumentException("Too small");
                removeQuote.setText("You will receive:\n"+TradeDisplay.amount(outputs[1],18)+" BKC + "+TradeDisplay.amount(outputs[0],6)+" mUSDT");
                String problem=removeProblem(s,lp); boolean approved=s.lpAllowance.compareTo(lp)>=0, valid=problem.isEmpty() && !lock && actionReady;
                approveRemove.setVisibility(approved?View.GONE:View.VISIBLE);remove.setVisibility(approved?View.VISIBLE:View.GONE);
                TradeButtons.state(approveRemove,"Approve LP",valid && !approved,blocked || !problem.isEmpty());
                TradeButtons.state(remove,"Remove Liquidity",valid && approved,blocked || !problem.isEmpty());
                if(!problem.equals(base))removeHint=problem;
            }
        } catch(Exception ignored) { if(percent>0)removeHint=s!=null && s.lpBalance.signum()==0?"No LP tokens available.":"Amount too small or data unavailable."; }
        if(lock || pending!=null) {
            String[] labels={"Approve mUSDT","Add Liquidity","Approve LP","Remove Liquidity"};
            Button[] buttons={approveAdd,add,approveRemove,remove};
            for(int i=0;i<buttons.length;i++)TradeButtons.state(buttons[i],labels[i],false,pending!=null);
        } else {
            if(quote==null && loading)TradeButtons.state(add,loadingText(),false,blocked);
            if(s==null && loading && percent>0)TradeButtons.state(remove,loadingText(),false,blocked);
        }
        if(!lock && pending==null) {
            if(addHint.isEmpty())addHint=base.isEmpty() && quote==null && !loading?"Refresh the quote to continue.":base;
            if(removeHint.isEmpty())removeHint=base;
        }
        TradeUi.hint(activity.findViewById(R.id.lp_add_hint),lock?"":addHint,true);
        TradeUi.hint(activity.findViewById(R.id.lp_remove_hint),lock?"":removeHint,true);
        renderPosition(s); history.bind(config,wallet,this::afterHistory);
    }
    private static String share(BigInteger lp,BigInteger total) { return TradeDisplay.decimal(new BigDecimal(lp).multiply(new BigDecimal("100")).divide(new BigDecimal(total),12,RoundingMode.DOWN)); }
    private void renderPosition(Snapshot s) {
        if(s==null) {
            for(int id:new int[]{R.id.lp_pooled_bkc,R.id.lp_pooled_musdt,R.id.lp_pool_share,R.id.lp_position_value})((TextView)activity.findViewById(id)).setText("—");
            fees.setText("—");return;
        }
        BigInteger pooledM=s.pool.musdtReserve.multiply(s.lpBalance).divide(s.supply), pooledB=s.pool.wbkcReserve.multiply(s.lpBalance).divide(s.supply);
        BigInteger value=pooledM.add(pooledB.multiply(s.pool.musdtReserve).divide(s.pool.wbkcReserve));
        ((TextView)activity.findViewById(R.id.lp_pooled_bkc)).setText(TradeDisplay.amount(pooledB,18));
        ((TextView)activity.findViewById(R.id.lp_pooled_musdt)).setText(TradeDisplay.amount(pooledM,6));
        ((TextView)activity.findViewById(R.id.lp_pool_share)).setText(share(s.lpBalance,s.supply)+"%");
        ((TextView)activity.findViewById(R.id.lp_position_value)).setText(TradeDisplay.amount(value,6)+" mUSDT");
        // Ownership changes invalidate the observation basis; do not attribute old pool growth to new LP.
        SharedPreferences prefs=activity.getSharedPreferences("lp_observation_v1",Context.MODE_PRIVATE);
        String prefix=wallet+"_"+config.pair()+"_"; BigInteger root=LiquidityMath.sqrt(s.pool.musdtReserve.multiply(s.pool.wbkcReserve));
        String oldLp=prefs.getString(prefix+"lp","");
        if(!oldLp.equals(s.lpBalance.toString()))prefs.edit().putString(prefix+"lp",s.lpBalance.toString())
                .putString(prefix+"root",root.toString()).putString(prefix+"supply",s.supply.toString()).apply();
        try {
            BigInteger earned=LiquidityMath.estimatedFeesMusdt(s.pool,s.supply,s.lpBalance,
                    new BigInteger(prefs.getString(prefix+"root",root.toString())),new BigInteger(prefs.getString(prefix+"supply",s.supply.toString())));
            fees.setText(TradeDisplay.amount(earned,6)+" mUSDT");
        } catch(Exception e) { fees.setText("—"); }
    }
    private void confirm(boolean adding,boolean approval) {
        if (locked() || SystemClock.elapsedRealtime() < quoteActionsEnabledAt) return;
        if(config==null || !config.hasLiquidity() || wallet.isEmpty() || !sameAccount(wallet) || unresolved()!=null) { toast("Check your account, deployment and unresolved transactions."); return; }
        if(quote==null || !adding && snapshot==null)return;
        try {
            Snapshot s=snapshot;
            BigInteger desiredM=adding?SwapQuoteMath.parseUnits(musdt.getText().toString(),6):BigInteger.ZERO;
            BigInteger desiredB=adding?SwapQuoteMath.parseUnits(bkc.getText().toString(),18):BigInteger.ZERO;
            BigInteger lp=adding?BigInteger.ZERO:LiquidityMath.portion(s.lpBalance,percent);
            LiquidityMath.AddQuote q=adding?LiquidityMath.add(quote.pool,quote.supply,desiredM,desiredB):null;
            BigInteger[] out=adding?null:LiquidityMath.remove(s.pool,s.supply,lp);
            BigInteger minM=LiquidityMath.minimum(adding?q.musdt:out[0]),minB=LiquidityMath.minimum(adding?q.bkc:out[1]),minLp=adding?LiquidityMath.minimum(q.lp):BigInteger.ZERO;
            if(minM.signum()==0 || minB.signum()==0)throw new IllegalArgumentException("Amount too small");
            String problem=adding?addProblem(s,desiredM,desiredB):removeProblem(s,lp); if(!problem.isEmpty()) { toast(problem); return; }
            boolean approved=s==null || (adding?s.musdtAllowance.compareTo(desiredM):s.lpAllowance.compareTo(lp))>=0;
            if(approval==approved) { toast(approved?"Allowance is sufficient. No further approval is needed.":"Approve this amount first."); return; }
            String action=approval?adding?"APPROVE_MUSDT":"APPROVE_LP":adding?"ADD":"REMOVE";
            final int id=generation; final String account=wallet;
            confirming=true; render();
            android.content.DialogInterface.OnClickListener onConfirm=(d,w) -> prepare(id,account,action,desiredM,desiredB,lp,minM,minB,minLp,q==null?null:q.lp);
            String token=adding?"mUSDT":"LP";
            String fields=adding?"Pool will use "+SwapQuoteMath.format(desiredB,18)+" BKC + "+SwapQuoteMath.format(desiredM,6)+" mUSDT\nEstimated LP tokens: "+SwapQuoteMath.format(q.lp,18)+" LPC\nMin LP tokens: "+SwapQuoteMath.format(minLp,18)+" LPC"
                    :"Remove: "+SwapQuoteMath.format(lp,18)+" LPC\nYou will receive: "+SwapQuoteMath.format(out[1],18)+" BKC + "+SwapQuoteMath.format(out[0],6)+" mUSDT";
            fields+="\nMin BKC: "+SwapQuoteMath.format(minB,18)+"\nMin mUSDT: "+SwapQuoteMath.format(minM,6)+"\nSlippage: 0.5%";
            AlertDialog dialog=approval?TradeVideoUi.approval(activity,token,SwapQuoteMath.format(adding?desiredM:lp,adding?6:18),onConfirm)
                    :TradeVideoUi.confirmation(activity,adding?"Confirm add liquidity":"Confirm remove liquidity",fields,onConfirm);
            dialog.setOnDismissListener(d -> { confirming=false; matchOther(); render(); });
            if(approval)TradeVideoUi.showApproval(dialog);else TradeVideoUi.show(dialog);
        } catch(Exception e) { toast("Refresh on-chain data and check the amount or removal percentage."); }
    }
    private void prepare(int id,String account,String action,BigInteger desiredM,BigInteger desiredB,BigInteger lp,
                         BigInteger minM,BigInteger minB,BigInteger minLp,BigInteger estimatedLp) {
        preparing=true; render();
        if(readSession!=null)readSession.cancel();
        TradeReadQueue.executeUser(() -> {
            String stage="Account";
            try {
                if(id!=generation || !sameAccount(account))throw new IllegalStateException("Account changed.");
                String key=TokenWalletHelper.getCurrentPrivateKey(activity.getApplicationContext());
                // Always check code/getters again for a financial action, even if display validation is cached.
                stage="Liquidity contract";
                client.validateLiquidity(key);
                long checked=SystemClock.elapsedRealtime();
                main.post(() -> { if(resumed && id==generation && sameAccount(account))validatedAt=checked; });
                stage="Pool and account data";
                Snapshot current=readSnapshot(key,account);
                boolean adding="ADD".equals(action)||"APPROVE_MUSDT".equals(action), approval=action.startsWith("APPROVE");
                String problem=adding?addProblem(current,desiredM,desiredB):removeProblem(current,lp);
                if(!problem.isEmpty())throw new TradeReadFailure.Preflight(problem);
                try {
                    if(adding) {
                        LiquidityMath.AddQuote q=LiquidityMath.add(current.pool,current.supply,desiredM,desiredB);
                        LiquiditySafety.addMinimums(q,minM,minB,minLp);
                    } else {
                        BigInteger[] out=LiquidityMath.remove(current.pool,current.supply,lp);
                        LiquiditySafety.removeMinimums(out,minM,minB);
                    }
                } catch(IllegalArgumentException e) {
                    throw new TradeReadFailure.Preflight("Pool estimates changed beyond the confirmed limits.");
                }
                if(!current.fresh())throw new TradeReadFailure.Preflight("Pre-send checks expired.");
                BigInteger amount=adding?desiredM:lp;
                if(!approval && (adding?current.musdtAllowance:current.lpAllowance).compareTo(amount)<0)throw new TradeReadFailure.Preflight("Insufficient allowance for this amount.");
                stage="Transaction encoding";
                String target=approval?adding?config.musdt():config.pair():config.liquidityRouter();
                long deadline=System.currentTimeMillis()/1000+300;
                String data=approval?BrokerSwapAbi.call("approve",new Address(config.liquidityRouter()),new Uint256(amount))
                        :adding?LiquidityAbi.add(desiredM,minM,minB,minLp,account,deadline):LiquidityAbi.remove(lp,minM,minB,account,deadline);
                BigInteger value=!approval && adding?desiredB:BigInteger.ZERO;
                TokenTxRecord record=new TokenTxRecord(); record.operationId=UUID.randomUUID().toString(); record.timestampMs=System.currentTimeMillis();
                record.type=approval?"APPROVAL":"LIQUIDITY"; record.liquidityAction=action; record.routerAddress=config.liquidityRouter(); record.chainId=config.chainId();
                record.deadlineSeconds=approval?0:deadline;
                record.contractAddress=approval?target:config.pair(); record.inputUnits=amount.toString();
                if(!approval)record.toContractAddress=config.musdt();
                record.fromSymbol=approval?adding?"mUSDT":"LP":adding?"BKC + mUSDT":"LP"; record.toSymbol=adding?"LP":"BKC + mUSDT";
                record.amountDisplay=approval?SwapQuoteMath.format(amount,adding?6:18):adding?SwapQuoteMath.format(desiredB,18)+" BKC + "+SwapQuoteMath.format(desiredM,6)+" mUSDT":SwapQuoteMath.format(lp,18)+" LP";
                record.musdtUnits=desiredM.toString(); record.bkcUnits=desiredB.toString(); record.lpUnits=lp.toString();
                record.minimumMusdtUnits=minM.toString(); record.minimumBkcUnits=minB.toString(); record.minimumLpUnits=minLp.toString(); record.slippageBps=50;
                if(estimatedLp!=null)record.estimatedLp=SwapQuoteMath.format(estimatedLp,18);
                main.post(() -> {
                    preparing=false;
                    if(activity.isDestroyed() || id!=generation || !sameAccount(account))return;
                    if(!current.fresh()) { error="Checks expired. No transaction was sent. Refresh the quote and confirm again."; toast(error); render(); return; }
                    if(!SwapOperationRunner.submit(activity,config,account,record,target,data,value,this::completed))toast("A transaction is already in progress.");
                    render();
                });
            } catch(Exception e) {
                final String message=e instanceof TradeReadFailure.Preflight?e.getMessage():stage+" check failed. "+TradeReadFailure.describe(e);
                main.post(() -> {
                    preparing=false;
                    if(activity.isDestroyed())return;
                    if(id==generation && sameAccount(account)) { error=message+" No transaction was sent. Refresh the quote and confirm again."; matchOther(); toast(error); render(); }
                });
            }
        });
    }
    private void completed(TokenTxRecord record) {
        if(activity.isDestroyed())return;
        if("ADD".equals(record.liquidityAction) && "SUCCESS".equals(record.status)) {
            updating=true;
            try {bkc.setText("");musdt.setText("");} finally {updating=false;}
        }
        toast(record.liquidityAction!=null && record.liquidityAction.startsWith("APPROVE") && "SUCCESS".equals(record.status)
                ? "Approval successful" : "SUCCESS".equals(record.status)?"ADD".equals(record.liquidityAction)?"Liquidity added":"Liquidity removed successfully":VideoTradePresentation.tradeResult(record.status));
        if(!"FAILED".equals(record.status)) { snapshot=null; fetch(); }
        matchOther(); render();
    }
    private void resumePending() {
        if (config == null || wallet.isEmpty()) return;
        TokenTxRecord pending = unresolved();
        if (pending == null || !SwapReceipt.validHash(pending.txHash)) return;
        SwapOperationRunner.resume(activity, config, wallet, pending, record -> {
            if (activity.isDestroyed()) return;
            if (record.isLiquidity()) {
                completed(record);
            } else {
                snapshot = null;
                afterHistory();
            }
        });
    }

    private void afterHistory() { render(); if(!SwapOperationRunner.active(wallet))fetch(); }
    private String loadingText() {
        return "Loading quote...";
    }
    private void toast(String message) { Toast.makeText(activity,message,Toast.LENGTH_LONG).show(); }
}
