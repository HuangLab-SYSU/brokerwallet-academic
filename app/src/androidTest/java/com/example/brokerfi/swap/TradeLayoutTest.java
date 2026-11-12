package com.example.brokerfi.swap;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.brokerfi.R;
import com.example.brokerfi.token.TokenTxRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Isolated layout/callback tests. Fixtures never access accounts, sign or contact a network. */
@RunWith(AndroidJUnit4.class)
public class TradeLayoutTest {
    private Context context() {
        return new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(),R.style.Theme_BrokerFi);
    }
    private View inflate(int layout) {return LayoutInflater.from(context()).inflate(layout,null,false);}
    private void main(Runnable action) {InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    @Test public void legacySwapDefaultsHideTradeOnlyUi() {main(() -> {
        View root=inflate(R.layout.activity_token_swap);
        assertNull(root.findViewById(R.id.broker_swap_panel));
        assertNull(root.findViewById(R.id.trade_liquidity_content));
        assertFalse(root.findViewById(R.id.token_confirm_button).isEnabled());
        assertNull(root.findViewById(R.id.video_rate));
    });}
    @Test public void swapAndLiquidityLayoutsKeepTheirOwnControls() {main(() -> {
        View swapPage = inflate(R.layout.activity_broker_swap);
        View liquidityPage = inflate(R.layout.activity_broker_liquidity);
        Button swap = swapPage.findViewById(R.id.token_confirm_button);
        Button refreshQuote = swapPage.findViewById(R.id.trade_refresh_quote);
        Button refreshPoolQuote = liquidityPage.findViewById(R.id.lp_refresh_quote);
        assertEquals("Swap", swap.getText().toString());
        assertFalse(swap.isEnabled());
        assertEquals("Refresh quote", refreshQuote.getText().toString());
        assertEquals("Refresh quote", refreshPoolQuote.getText().toString());
        assertNotNull(swapPage.findViewById(R.id.token_pay_token_selector));
        assertNotNull(swapPage.findViewById(R.id.token_receive_token_selector));
        assertNull(swapPage.findViewById(R.id.trade_liquidity_content));
        assertNull(liquidityPage.findViewById(R.id.trade_swap_content));
        assertNotNull(swapPage.findViewById(R.id.trade_swap_history_toggle));
        assertNotNull(liquidityPage.findViewById(R.id.lp_history_toggle));
        for(int id:new int[]{R.id.lp_approve_add,R.id.lp_add,R.id.lp_approve_remove,R.id.lp_remove}) {
            assertTrue(liquidityPage.findViewById(id) instanceof Button);
            assertFalse(liquidityPage.findViewById(id).isEnabled());
        }
        assertEquals("Enter one amount to preview the pool ratio",
                ((TextView)liquidityPage.findViewById(R.id.lp_add)).getText().toString());
        assertNotNull(liquidityPage.findViewById(R.id.lp_pool_share));
        assertNotNull(liquidityPage.findViewById(R.id.lp_position_change));
        assertEquals(View.GONE,liquidityPage.findViewById(R.id.lp_history_rows).getVisibility());
    });}
    @Test public void homeOwnsActionsAndAssetsShowsTableAndManagement() {main(() -> {
        View home=inflate(R.layout.activity_main);
        assertNotNull(home.findViewById(R.id.send));assertNotNull(home.findViewById(R.id.receiveicon));
        assertNotNull(home.findViewById(R.id.swap));assertNotNull(home.findViewById(R.id.home_liquidity));
        assertNotNull(home.findViewById(R.id.home_bkc_row));assertNotNull(home.findViewById(R.id.home_wbkc_row));
        View assets=inflate(R.layout.activity_token_detail);
        assertNotNull(assets.findViewById(R.id.wallet_asset_table));
        Button manage = assets.findViewById(R.id.token_manage_tokens_button);
        assertEquals("Manage tokens", manage.getText().toString());
        assertNotNull(manage.getBackground());
        assertFalse(allText(assets).contains("History"));
        assertEquals("Assets",context().getString(R.string.token_home_label));
    });}
    @Test public void restoredHomepageLayoutRemainsAvailable() {main(() -> assertNotNull(inflate(R.layout.activity_swap)));}
    @Test public void liquidityContentCanScrollToRemovalAndHistory() {main(() -> {
        View root=trade(true);measure(root,320,640);
        NestedScrollView scroll=root.findViewById(R.id.trade_scroll);
        assertTrue(scroll.getChildAt(0).getHeight()>scroll.getHeight());
        scroll.scrollTo(0,scroll.getChildAt(0).getHeight());assertTrue(scroll.getScrollY()>0);
    });}
    @Test public void compactHistoryRemainsReadableAtLargeFontAndOpensOnce() {main(() -> {
        Context base=context();Configuration configuration=new Configuration(base.getResources().getConfiguration());configuration.fontScale=1.5f;
        Context large=new ContextThemeWrapper(base.createConfigurationContext(configuration),R.style.Theme_BrokerFi);
        TokenTxRecord r=example();r.status="UNKNOWN";r.amountDisplay="1234567.123456789";r.actualOutput=null;
        final int[] clicks={0};LinearLayout row=TradeVideoUi.historyRow(large,r,v -> clicks[0]++);
        measure(row,280,0);readable(row);row.performClick();assertEquals(1,clicks[0]);
        assertTrue(allText(row).contains("Do not send again"));assertFalse(allText(row).contains("详情"));
        View root=LayoutInflater.from(large).inflate(R.layout.activity_broker_liquidity,null,false);
        measure(root,320,640);TextView button=root.findViewById(R.id.lp_add);
        assertTrue(button.getHeight()>=button.getLayout().getHeight()+button.getCompoundPaddingTop()+button.getCompoundPaddingBottom());
    });}
    @Test public void cancelAndConfirmHaveSeparateCallbacksAndDetailsCloseWorks() {
        try(ActivityScenario<TradeVisualPreviewActivity> scenario=ActivityScenario.launch(TradeVisualPreviewActivity.class)) {
            scenario.onActivity(activity -> {
                activity.setContentView(R.layout.activity_broker_swap);final int[] sends={0};
                AlertDialog cancel=TradeVideoUi.confirmation(activity,"Confirm swap","Pay: 10 BKC\nMin Receive: 46.55758 mUSDT",(d,w) -> sends[0]++);
                TradeVideoUi.show(cancel);cancel.getWindow().getDecorView().findViewWithTag("video-cancel").performClick();
                assertEquals(0,sends[0]);assertFalse(cancel.isShowing());
                AlertDialog confirm=TradeVideoUi.confirmation(activity,"Confirm swap","Pay: 10 BKC",(d,w) -> sends[0]++);
                TradeVideoUi.show(confirm);confirm.getWindow().getDecorView().findViewWithTag("video-confirm").performClick();
                assertEquals(1,sends[0]);assertFalse(confirm.isShowing());
                AlertDialog details=TradeVideoUi.recordBuilder(activity,example()).create();TradeVideoUi.show(details);
                assertNotNull(details.getWindow().getDecorView().findViewWithTag("video-close"));
                TextView hash=details.getWindow().getDecorView().findViewWithTag("video-hash");
                assertEquals(example().txHash,hash.getText().toString());hash.performClick();
                android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
                assertEquals(example().txHash,clipboard.getPrimaryClip().getItemAt(0).getText().toString());
                details.getWindow().getDecorView().findViewWithTag("video-close").performClick();assertFalse(details.isShowing());
            });
        }
    }
    @Test public void assetHistoryRecyclingRestoresLegacyAndTradeAmounts() {main(() -> {
        androidx.recyclerview.widget.RecyclerView.Adapter adapter=new com.example.brokerfi.token.TokenTxAdapter();
        com.example.brokerfi.token.TokenTxAdapter records=(com.example.brokerfi.token.TokenTxAdapter)adapter;
        android.widget.FrameLayout parent=new android.widget.FrameLayout(context());TokenTxRecord trade=example();
        records.setRecords(java.util.Collections.singletonList(trade));
        androidx.recyclerview.widget.RecyclerView.ViewHolder holder=adapter.onCreateViewHolder(parent,0);adapter.onBindViewHolder(holder,0);
        assertEquals("Swap",((TextView)holder.itemView.findViewById(R.id.token_tx_type)).getText().toString());
        assertEquals("10 BKC",((TextView)holder.itemView.findViewById(R.id.token_tx_amount)).getText().toString());
        assertEquals(View.GONE,holder.itemView.findViewById(R.id.token_tx_swap_amount).getVisibility());
        TokenTxRecord legacy=example();legacy.status=null;legacy.type="WRAP";
        records.setRecords(java.util.Collections.singletonList(legacy));adapter.onBindViewHolder(holder,0);
        assertEquals(View.VISIBLE,holder.itemView.findViewById(R.id.token_tx_amount).getVisibility());
        records.setRecords(java.util.Collections.singletonList(trade));adapter.onBindViewHolder(holder,0);
        assertEquals("10 BKC",((TextView)holder.itemView.findViewById(R.id.token_tx_amount)).getText().toString());
    });}
    @Test public void nativeApprovalShowsExactAmountAndRequiresItsOwnConfirmation() {
        try(ActivityScenario<TradeVisualPreviewActivity> scenario=ActivityScenario.launch(TradeVisualPreviewActivity.class)) {
            final AlertDialog[] shown={null};final int[] approvals={0};
            scenario.onActivity(activity -> {
                activity.setContentView(R.layout.activity_broker_swap);
                shown[0]=TradeVideoUi.approval(activity,"mUSDT","46.9",(d,w) -> approvals[0]++);TradeVideoUi.showApproval(shown[0]);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                View decor=shown[0].getWindow().getDecorView();
                assertTrue(allText(decor).contains("Allow the router to spend up to 46.9 mUSDT from your wallet."));
                assertTrue(allText(decor).contains("This is a one-time approval for this amount."));
                save(decor,"10-approve-dialog-content.png");shown[0].getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                assertEquals(0,approvals[0]);assertFalse(shown[0].isShowing());
                shown[0]=TradeVideoUi.approval(activity,"mUSDT","46.9",(d,w) -> approvals[0]++);TradeVideoUi.showApproval(shown[0]);
                shown[0].getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {assertEquals(1,approvals[0]);assertFalse(shown[0].isShowing());});
        }
    }
    @Test public void renderVideoReferenceFixturesWithoutAccountAccess() {main(() -> {
        View swap=trade(false);Context c=swap.getContext();
        set(swap,R.id.token_pay_amount,"10");set(swap,R.id.token_receive_amount,"46.7915");
        ((TextView)swap.findViewById(R.id.token_receive_amount)).setTextColor(0xff111827);
        set(swap,R.id.token_pay_symbol,"BKC");set(swap,R.id.token_receive_symbol,"mUSDT");set(swap,R.id.token_receive_token_icon,"M");
        set(swap,R.id.token_pay_balance,"Balance: 116.19 BKC");set(swap,R.id.token_receive_balance,"Balance: 197,972.57 mUSDT");
        set(swap,R.id.video_rate,"1 BKC = ~4.679153 mUSDT");set(swap,R.id.broker_swap_details,"Min received: 46.55758 mUSDT");
        swap.findViewById(R.id.video_slip_05).setSelected(true);((TextView)swap.findViewById(R.id.video_slip_05)).setTypeface(null,android.graphics.Typeface.BOLD);TradeButtons.state(swap.findViewById(R.id.token_confirm_button),"Swap",true,false);
        LinearLayout rows=swap.findViewById(R.id.trade_swap_history_rows);rows.setVisibility(View.VISIBLE);
        Button toggle=swap.findViewById(R.id.trade_swap_history_toggle);toggle.setVisibility(View.VISIBLE);toggle.setText("Swap History");toggle.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.video_arrow_up,0);
        for(int i=0;i<3;i++){TokenTxRecord r=example();if(i>0){r.type="WRAP";r.toSymbol="wBKC";}rows.addView(TradeVideoUi.historyRow(c,r,v -> {}));}
        rows.setVisibility(View.GONE);toggle.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.video_arrow_down,0);
        measure(swap,360,756);save(swap,"01-swap-reference.png");
        rows.setVisibility(View.VISIBLE);toggle.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.video_arrow_up,0);measure(swap,360,756);
        NestedScrollView scroll=swap.findViewById(R.id.trade_scroll);scroll.scrollTo(0,scroll.getChildAt(0).getHeight());save(swap,"02-swap-history-reference.png");
        View confirmation=TradeVideoUi.confirmationView(c,"Confirm swap","Pay: 10 BKC\nMin Receive: 46.55758 mUSDT\nRate: 1 BKC = ~4.679153 mUSDT\nSlippage: 0.5%\nPrice impact: 0.07%");
        measure(confirmation,340,0);readable((ViewGroup)confirmation);save(confirmation,"03-confirm-swap-content.png");
        View details=TradeVideoUi.recordView(c,example());measure(details,340,0);readable((ViewGroup)details);save(details,"04-transaction-details-content.png");
        View lp=trade(true);set(lp,R.id.lp_bkc_amount,"10");set(lp,R.id.lp_musdt_amount,"46.9");
        set(lp,R.id.lp_add_quote,"Pool will use 10 BKC + 46.9 mUSDT\nEstimated LP tokens: 0 LPC\nEstimated pool share: 0.07%");
        set(lp,R.id.lp_owned,"Your LP tokens: 19.47 LPC");set(lp,R.id.lp_pooled_bkc,"37.826");set(lp,R.id.lp_pooled_musdt,"177.4");set(lp,R.id.lp_pool_share,"0.27%");
        set(lp,R.id.lp_position_value,"354.8 mUSDT");set(lp,R.id.lp_fees,"0.01 mUSDT");
        set(lp,R.id.lp_remove_percent,"Remove 21% of your LP tokens");((android.widget.SeekBar)lp.findViewById(R.id.lp_remove_slider)).setProgress(21);
        set(lp,R.id.lp_remove_quote,"You will receive:\n7.9435 BKC + 37.25 mUSDT");TradeButtons.state(lp.findViewById(R.id.lp_remove),"Remove Liquidity",true,false);
        Button approve=lp.findViewById(R.id.lp_approve_add);approve.setVisibility(View.VISIBLE);lp.findViewById(R.id.lp_add).setVisibility(View.GONE);TradeButtons.state(approve,"Approve mUSDT",true,false);
        measure(lp,360,756);save(lp,"05-liquidity-approval-reference.png");
        approve.setVisibility(View.GONE);lp.findViewById(R.id.lp_add).setVisibility(View.VISIBLE);TradeButtons.state(lp.findViewById(R.id.lp_add),"Add Liquidity",true,false);
        measure(lp,360,756);save(lp,"06-liquidity-ready-reference.png");
        LinearLayout history=lp.findViewById(R.id.lp_history_rows);history.setVisibility(View.VISIBLE);
        for(int i=0;i<4;i++){TokenTxRecord r=example();r.liquidityAction=i%2==0?"REMOVE":"ADD";r.actualBkc="10.04";r.actualMusdt="47.1";r.actualLp="5.17";history.addView(TradeVideoUi.historyRow(c,r,v -> {}));}
        ((Button)lp.findViewById(R.id.lp_history_toggle)).setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.video_arrow_up,0);
        measure(lp,360,756);NestedScrollView lpScroll=lp.findViewById(R.id.trade_scroll);
        View positionPanel=(View)lp.findViewById(R.id.lp_pooled_bkc).getParent().getParent().getParent();
        int y=positionPanel.getTop()+lp.findViewById(R.id.trade_liquidity_content).getTop();lpScroll.scrollTo(0,y);
        save(lp,"07-liquidity-position-reference.png");lpScroll.scrollTo(0,lpScroll.getChildAt(0).getHeight());save(lp,"08-liquidity-history-reference.png");
        View assets=inflate(R.layout.activity_token_detail);
        LinearLayout table=assets.findViewById(R.id.wallet_asset_table);
        for (String[] values : new String[][]{{"BKC", "21.0840685 BKC"}, {"wBKC", "4.32 wBKC"}, {"mUSDT", "197,972.57 mUSDT"}}) {
            View asset=LayoutInflater.from(c).inflate(R.layout.wallet_asset_row,table,false);
            set(asset,R.id.asset_row_name,values[0]);set(asset,R.id.asset_row_balance,values[1]);
            table.addView(asset);
        }
        measure(assets,360,756);save(assets,"09-assets-reference.png");
    });}
    private View trade(boolean liquidity) {
        View root=inflate(liquidity ? R.layout.activity_broker_liquidity : R.layout.activity_broker_swap);
        root.findViewById(R.id.token_info_button).setVisibility(View.GONE);
        set(root,R.id.token_page_title,liquidity ? "Provide Liquidity" : "Swap");
        return root;
    }
    private TokenTxRecord example() {
        TokenTxRecord r=new TokenTxRecord();r.type="SWAP";r.status="SUCCESS";r.amountDisplay="10";r.actualOutput="46.79";r.fromSymbol="BKC";r.toSymbol="mUSDT";
        r.txHash="0x"+new String(new char[64]).replace('\0','a');r.timestampMs=1784900400000L;return r;
    }
    private void set(View root,int id,String value){((TextView)root.findViewById(id)).setText(value);}
    private void measure(View view,int widthDp,int heightDp) {
        int w=TradeUi.dp(view.getContext(),widthDp),h=TradeUi.dp(view.getContext(),heightDp);
        view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,heightDp==0?View.MeasureSpec.UNSPECIFIED:View.MeasureSpec.EXACTLY));view.layout(0,0,w,view.getMeasuredHeight());
    }
    private void readable(ViewGroup group) {
        for(int i=0;i<group.getChildCount();i++) {View child=group.getChildAt(i);if(child.getVisibility()!=View.VISIBLE)continue;
            if(child instanceof TextView){TextView text=(TextView)child;assertNotNull(text.getLayout());assertTrue("Clipped text: "+text.getText(),text.getHeight()>=text.getLayout().getHeight()+text.getCompoundPaddingTop()+text.getCompoundPaddingBottom());}
            else if(child instanceof ViewGroup)readable((ViewGroup)child);
        }
    }
    private String allText(View view) {
        if(view instanceof TextView)return ((TextView)view).getText().toString();StringBuilder result=new StringBuilder();
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)result.append(allText(((ViewGroup)view).getChildAt(i))).append('\n');return result.toString();
    }
    private void save(View view,String name) {
        Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(0xffF7F8FC);view.draw(canvas);
        java.io.File dir=new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"video-replica-fixtures");assertTrue(dir.isDirectory() || dir.mkdirs());
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}
        catch(java.io.IOException e){throw new AssertionError(e);}finally{bitmap.recycle();}
    }
}
