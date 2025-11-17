package com.example.brokerfi.xc.menu;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;



import com.example.brokerfi.R;
import com.example.brokerfi.xc.AboutActivity;
import com.example.brokerfi.xc.AtvActivity;

import com.example.brokerfi.xc.EmulatorActivity;
import com.example.brokerfi.xc.MainActivity;
import com.example.brokerfi.xc.NewsActivity;
import com.example.brokerfi.xc.NewsActivity2;
import com.example.brokerfi.xc.QRCode.Capture;
import com.example.brokerfi.xc.ReceiveActivity;

import com.example.brokerfi.xc.SelectAccountActivity;
import com.example.brokerfi.xc.SettingActivity;
import com.example.brokerfi.xc.WelcomeBackActivity;
import com.example.brokerfi.xc.tool.UnitConverter;
import com.google.zxing.integration.android.IntentIntegrator;


public class NavigationHelper{
    private ImageView menu;
    private ImageView notice;
    private RelativeLayout action_bar;
    private Context context;

    private boolean isIcon1 = true;
    private boolean isPopupVisible = false;
    private View customView;
    private int action_bar_height;
    private RelativeLayout sendlist;
    private RelativeLayout receivelist;
    private RelativeLayout activitylist;
    private RelativeLayout setlist;
    private RelativeLayout supportlist;
    private RelativeLayout about;
    private RelativeLayout locklist;
    private PopupWindow popupWindow;
    boolean hasExecuted = false;


    public NavigationHelper(ImageView menu, RelativeLayout action_bar,Context context,ImageView notificationBtn) {
        this.menu = menu;
        this.action_bar = action_bar;
        this.context = context;
        this.notice = notificationBtn;


        int status_bar_height = getStatusBarHeight(context);
        int marginTop = status_bar_height + UnitConverter.dpToSp(context,2);

        if (action_bar.getParent() instanceof RelativeLayout) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, marginTop, 0, 0);
            this.action_bar.setLayoutParams(params);
        } else if (action_bar.getParent() instanceof LinearLayout) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, marginTop, 0, 0);
            this.action_bar.setLayoutParams(params);
        }

        customView = LayoutInflater.from(context).inflate(R.layout.menu, null);

        this.action_bar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!hasExecuted) {
                    action_bar_height = action_bar.getHeight();
                    int height;
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int screenHeight = displayMetrics.heightPixels;
                    Rect rect = new Rect();
                    ((Activity) context).getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                    int usableScreenHeight = rect.height();
                    if(usableScreenHeight == screenHeight){
                        height = (int) (screenHeight - action_bar_height );
                    }else {
                        height = (int) (screenHeight - action_bar_height - marginTop );
                    }

                    popupWindow = new PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT, height, false);
                    hasExecuted = true;
                }
            }
        });

        intCustomView();
        menu.setOnClickListener(view -> {
            if (isKeyboardShown()) {
                hideKeyboard();
                new Handler().postDelayed(() -> {
                    toggleMenuIcon();
                    togglePopupVisibility();
                }, 100);
            }else{
                toggleMenuIcon();
                togglePopupVisibility();

            }
        });

    }

    private void intCustomView(){
        sendlist = customView.findViewById(R.id.sendlist);
        receivelist = customView.findViewById(R.id.receivelist);
        activitylist = customView.findViewById(R.id.activitylist);
        setlist = customView.findViewById(R.id.setlist);
        supportlist = customView.findViewById(R.id.supportlist);
        about = customView.findViewById(R.id.about);
        locklist = customView.findViewById(R.id.locklist);
        about.setOnClickListener(v -> {
            context.startActivity(new Intent(context, AboutActivity.class));
        });
        notice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(context, NewsActivity2.class);
                //跳转
                context.startActivity(intent);
            }
        });
        supportlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(context, EmulatorActivity.class);
                //跳转
                context.startActivity(intent);
//                String url = "https://www.blockemulator.com";
//
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//
//
//                if (intent.resolveActivity(context.getPackageManager()) != null) {
//
//                    context.startActivity(intent);
//                } else {
//
//                    Toast.makeText(context, "打开网页失败，请手动打开https://www.blockemulator.com", Toast.LENGTH_LONG).show();
//                }
            }
        });

        locklist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, WelcomeBackActivity.class);
                context.startActivity(intent);
            }
        });

        sendlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                IntentIntegrator intentIntegrator = new IntentIntegrator((Activity) context);
                intentIntegrator.setPrompt("For flash use volume up key");
                intentIntegrator.setBeepEnabled(true);
                intentIntegrator.setOrientationLocked(true);
                intentIntegrator.setCaptureActivity(Capture.class);
                intentIntegrator.initiateScan();

            }
        });
        receivelist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, ReceiveActivity.class);
                context.startActivity(intent);

            }
        });
        activitylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, AtvActivity.class);
                context.startActivity(intent);

            }
        });
        setlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, SelectAccountActivity.class);
                context.startActivity(intent);

            }
        });
    }




    private void toggleMenuIcon() {
        if (isIcon1) {
            menu.setImageResource(R.drawable.up_circle);
        } else {
            menu.setImageResource(R.drawable.action_menu_30);
        }
        isIcon1 = !isIcon1;
    }

    private void togglePopupVisibility() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;

        if (!isPopupVisible) {
            int yOffset = (int)(action_bar_height - menu.getHeight()) / 2;
            popupWindow.showAsDropDown(menu, 0, yOffset);

        } else {
            popupWindow.dismiss();
        }
        isPopupVisible = !isPopupVisible;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(menu.getWindowToken(), 0);
    }
    //检查输入法是否在显示
    private boolean isKeyboardShown() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        return imm.isAcceptingText();
    }
    //获取顶部状态栏的高度
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }






}
