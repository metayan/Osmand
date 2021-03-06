package net.osmand.plus.settings.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.enums.DayNightMode;
import net.osmand.plus.routing.RouteLineDrawInfo;
import net.osmand.plus.routing.cards.RouteLineColorCard;
import net.osmand.plus.routing.cards.RouteLineColorCard.OnMapThemeUpdateListener;
import net.osmand.plus.routing.cards.RouteLineColorCard.OnSelectedColorChangeListener;
import net.osmand.plus.routing.cards.RouteLineWidthCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.CustomColorBottomSheet.ColorPickerListener;
import net.osmand.plus.track.TrackAppearanceFragment.OnNeedScrollListener;

public class RouteLineAppearanceFragment extends ContextMenuScrollFragment implements ColorPickerListener, OnSelectedColorChangeListener, OnMapThemeUpdateListener {

	public static final String TAG = RouteLineAppearanceFragment.class.getName();

	private static final String INIT_MAP_THEME = "init_map_theme";
	private static final String SELECTED_MAP_THEME = "selected_map_theme";

	private RouteLineDrawInfo routeLineDrawInfo;

	private int toolbarHeightPx;
	private DayNightMode initMapTheme;
	private DayNightMode selectedMapTheme;

	private View buttonsShadow;
	private View controlButtons;
	private View toolbarContainer;

	private RouteLineColorCard colorCard;
	private RouteLineWidthCard widthCard;

	@Override
	public int getMainLayoutId() {
		return R.layout.route_line_appearance;
	}

	@Override
	public int getHeaderViewHeight() {
		return 0;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return false;
	}

	@Override
	public int getToolbarHeight() {
		return isPortrait() ? toolbarHeightPx : 0;
	}

	@Override
	public float getMiddleStateKoef() {
		return 0.5f;
	}

	@Override
	public int getInitialMenuState() {
		return MenuState.HALF_SCREEN;
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public boolean shouldShowMapControls(int menuState) {
		return menuState == MenuState.HALF_SCREEN;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		toolbarHeightPx = getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar);

		if (savedInstanceState != null) {
			routeLineDrawInfo = new RouteLineDrawInfo(savedInstanceState);
			initMapTheme = DayNightMode.valueOf(savedInstanceState.getString(INIT_MAP_THEME));
			selectedMapTheme = DayNightMode.valueOf(savedInstanceState.getString(SELECTED_MAP_THEME));
		} else {
			initMapTheme = getMyApplication().getSettings().DAYNIGHT_MODE.get();
			selectedMapTheme = initMapTheme;
		}

		requireMapActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				dismiss();
			}
		});
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			toolbarContainer = view.findViewById(R.id.context_menu_toolbar_container);
			buttonsShadow = view.findViewById(R.id.buttons_shadow);
			controlButtons = view.findViewById(R.id.control_buttons);
			if (isPortrait()) {
				updateCardsLayout();
			} else {
				int widthNoShadow = getLandscapeNoShadowWidth();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				controlButtons.setLayoutParams(params);
			}
			initContent(view);
		}
		return view;
	}

	private void initContent(@NonNull View view) {
		setupCards();
		setupToolbar();
		setupButtons(view);
		setupScrollShadow();
		enterAppearanceMode();
		openMenuHalfScreen();
		calculateLayout();
	}

	private void calculateLayout() {
		runLayoutListener(new Runnable() {
			@Override
			public void run() {
				updateMapControlsPos(RouteLineAppearanceFragment.this, getViewY(), true);
				initVisibleRect();
			}
		});
	}

	private void setupCards() {
		MapActivity mapActivity = requireMapActivity();
		ViewGroup cardsContainer = getCardsContainer();
		cardsContainer.removeAllViews();

		colorCard = new RouteLineColorCard(mapActivity, this, routeLineDrawInfo, initMapTheme, selectedMapTheme);
		cardsContainer.addView(colorCard.build(mapActivity));

		widthCard = new RouteLineWidthCard(mapActivity, routeLineDrawInfo, createScrollListener());
		cardsContainer.addView(widthCard.build(mapActivity));
	}

	private OnNeedScrollListener createScrollListener() {
		return new OnNeedScrollListener() {

			@Override
			public void onVerticalScrollNeeded(int y) {
				View view = widthCard.getView();
				if (view != null) {
					int resultYPosition = view.getTop() + y;
					int dialogHeight = getInnerScrollableHeight();
					ScrollView scrollView = (ScrollView) getBottomScrollView();
					if (resultYPosition > (scrollView.getScrollY() + dialogHeight)) {
						scrollView.smoothScrollTo(0, resultYPosition - dialogHeight);
					}
				}
			}

			private int getInnerScrollableHeight() {
				int totalScreenHeight = getViewHeight() - getMenuStatePosY(getCurrentMenuState());
				int frameTotalHeight = controlButtons.getHeight() + buttonsShadow.getHeight();
				return totalScreenHeight - frameTotalHeight;
			}
		};
	}

	private void setupToolbar() {
		ImageView closeButton = toolbarContainer.findViewById(R.id.close_button);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		closeButton.setImageResource(AndroidUtils.getNavigationIconResId(toolbarContainer.getContext()));
		updateToolbarVisibility(toolbarContainer);
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (Build.VERSION.SDK_INT >= 23 && !isNightMode() && view != null) {
			view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}
		return isNightMode() ? R.color.divider_color_dark : R.color.divider_color_light;
	}

	@Override
	public float getToolbarAlpha(int y) {
		return isPortrait() ? 1f : 0f;
	}

	private void setupButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.bg_color));
		View saveButton = view.findViewById(R.id.right_bottom_button);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getTargetFragment() instanceof OnApplyRouteLineListener) {
					((OnApplyRouteLineListener) getTargetFragment()).applyRouteLineAppearance(routeLineDrawInfo);
				}
				dismiss();
			}
		});

		View cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			}
		});

		UiUtilities.setupDialogButton(isNightMode(), cancelButton, DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		UiUtilities.setupDialogButton(isNightMode(), saveButton, DialogButtonType.PRIMARY, R.string.shared_string_apply);

		AndroidUiHelper.updateVisibility(saveButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	private void setupScrollShadow() {
		final View scrollView = getBottomScrollView();
		scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
			@Override
			public void onScrollChanged() {
				boolean scrollToBottomAvailable = scrollView.canScrollVertically(1);
				if (scrollToBottomAvailable) {
					showShadowButton();
				} else {
					hideShadowButton();
				}
			}
		});
	}

	private void showShadowButton() {
		buttonsShadow.setVisibility(View.VISIBLE);
		buttonsShadow.animate()
				.alpha(0.8f)
				.setDuration(200)
				.setListener(null);
	}

	private void hideShadowButton() {
		buttonsShadow.animate()
				.alpha(0f)
				.setDuration(200);
	}

	private void initVisibleRect() {
		MapActivity ctx = getMapActivity();
		int screenHeight = AndroidUtils.getScreenHeight(ctx);
		int screenWidth = AndroidUtils.getScreenWidth(ctx);
		int statusBarHeight = AndroidUtils.getStatusBarHeight(ctx);
		int centerX;
		int centerY;
		if (isPortrait()) {
			centerX = screenWidth / 2;
			centerY = (getViewY() + toolbarContainer.getHeight() + statusBarHeight) / 2;
		} else {
			boolean isRtl = AndroidUtils.isLayoutRtl(ctx);
			int dialogWidth = getLandscapeNoShadowWidth();
			int left = isRtl ? 0 : dialogWidth;
			int right = isRtl ? screenWidth - dialogWidth : screenWidth;
			centerX = (left + right) / 2;
			centerY = (screenHeight + statusBarHeight) / 2 ;
		}
		routeLineDrawInfo.setCenterX(centerX);
		routeLineDrawInfo.setCenterY(centerY);
		routeLineDrawInfo.setScreenHeight(screenHeight);
	}

	@Override
	public void onResume() {
		super.onResume();
		setDrawInfoOnRouteLayer(routeLineDrawInfo);
	}

	@Override
	public void onPause() {
		super.onPause();
		setDrawInfoOnRouteLayer(null);
	}

	private void setDrawInfoOnRouteLayer(@Nullable RouteLineDrawInfo drawInfo) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getRouteLayer().setRouteLineDrawInfo(drawInfo);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(INIT_MAP_THEME, initMapTheme.name());
		outState.putString(SELECTED_MAP_THEME, selectedMapTheme.name());
		routeLineDrawInfo.saveToBundle(outState);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitAppearanceMode();
	}

	private void enterAppearanceMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			AndroidUiHelper.setVisibility(mapActivity, portrait ? View.INVISIBLE : View.GONE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info);
		}
	}

	private void exitAppearanceMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info,
					R.id.map_search_button);
			changeMapTheme(initMapTheme);
		}
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackgroundDrawable(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, isNightMode(), R.color.list_background_color_light, R.color.list_background_color_dark);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.color.list_background_color_light, R.color.list_background_color_dark);
			}
		}
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		colorCard.onColorSelected(prevColor, newColor);
	}

	@Override
	public void onSelectedColorChanged() {
		if (widthCard != null) {
			widthCard.updateItems();
		}
		if (getMapActivity() != null) {
			getMapActivity().refreshMap();
		}
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity,
	                                   @NonNull RouteLineDrawInfo drawInfo,
	                                   @NonNull Fragment target) {
		try {
			RouteLineAppearanceFragment fragment = new RouteLineAppearanceFragment();
			fragment.setTargetFragment(target, 0);
			fragment.routeLineDrawInfo = new RouteLineDrawInfo(drawInfo);

			mapActivity.getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	public void onMapThemeUpdated(@NonNull DayNightMode mapTheme) {
		changeMapTheme(mapTheme);
	}

	private void changeMapTheme(@NonNull DayNightMode mapTheme) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.getSettings().DAYNIGHT_MODE.set(mapTheme);
			selectedMapTheme = mapTheme;
		}
	}

	public interface OnApplyRouteLineListener {
		void applyRouteLineAppearance(@NonNull RouteLineDrawInfo routeLineDrawInfo);
	}
}
