package net.osmand.plus.routepreparationmenu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerStartItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidRoadsRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidRoadsTypesRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.DividerItem;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.GpxLocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameterGroup;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.MuteSoundRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.OtherSettingsRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.RouteSimulationItem;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.ShowAlongTheRouteItem;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.router.GeneralRouter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.DRIVING_STYLE;

public class RouteOptionsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = "RouteOptionsBottomSheet";

	private OsmandApplication app;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;
	private RoutingOptionsHelper routingOptionsHelper;
	private ApplicationMode applicationMode;
	private MapActivity mapActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		settings = app.getSettings();
		routingHelper = app.getRoutingHelper();
		routingOptionsHelper = app.getRoutingOptionsHelper();
		mapActivity = getMapActivity();
		applicationMode = routingHelper.getAppMode();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(app.getString(R.string.shared_string_settings), nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light));

		List<LocalRoutingParameter> list = getRoutingParameters(applicationMode);

		for (final LocalRoutingParameter optionsItem : list) {
			if (optionsItem instanceof DividerItem) {
				items.add(new DividerStartItem(app));
			} else if (optionsItem instanceof MuteSoundRoutingParameter) {
				items.add(createMuteSoundItem(optionsItem));
			} else if (optionsItem instanceof ShowAlongTheRouteItem) {
				items.add(createShowAlongTheRouteItem(optionsItem));
			} else if (optionsItem instanceof RouteSimulationItem) {
				if (OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) != null) {
					items.add(createRouteSimulationItem(optionsItem));
				}
			} else if (optionsItem instanceof AvoidRoadsTypesRoutingParameter) {
				items.add(createAvoidRoadsTypesItem(optionsItem));
			} else if (optionsItem instanceof AvoidRoadsRoutingParameter) {
				items.add(createAvoidRoadsItem(optionsItem));
			} else if (optionsItem instanceof GpxLocalRoutingParameter) {
				items.add(createGpxRoutingItem(optionsItem));
			} else if (optionsItem instanceof OtherSettingsRoutingParameter) {
				items.add(createOtherSettingsRoutingItem(optionsItem));
			} else {
				inflateRoutingParameter(optionsItem);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				BottomSheetItemWithCompoundButton itemWithCompoundButton = (BottomSheetItemWithCompoundButton) item;
				itemWithCompoundButton.setChecked(itemWithCompoundButton.isChecked());
			}
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE
				&& resultCode == AvoidRoadsBottomSheetDialogFragment.OPEN_AVOID_ROADS_DIALOG_REQUEST_CODE) {
			dismiss();
		}
		if (requestCode == ShowAlongTheRouteBottomSheet.REQUEST_CODE
				&& resultCode == ShowAlongTheRouteBottomSheet.SHOW_CONTENT_ITEM_REQUEST_CODE) {
			dismiss();
		}
	}

	private BaseBottomSheetItem createMuteSoundItem(final LocalRoutingParameter optionsItem) {
		final BottomSheetItemWithCompoundButton[] muteSoundItem = new BottomSheetItemWithCompoundButton[1];
		muteSoundItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(!routingHelper.getVoiceRouter().isMute())
				.setDescription(getString(R.string.voice_announcements))
				.setIcon(getContentIcon((routingHelper.getVoiceRouter().isMute() ? optionsItem.getDisabledIconId() : optionsItem.getActiveIconId())))
				.setTitle(getString(R.string.shared_string_sound))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_switch_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
						boolean mt = !routingHelper.getVoiceRouter().isMute();
						settings.VOICE_MUTE.set(mt);
						routingHelper.getVoiceRouter().setMute(mt);
						muteSoundItem[0].setChecked(!routingHelper.getVoiceRouter().isMute());
						muteSoundItem[0].setIcon(getContentIcon((routingHelper.getVoiceRouter().isMute() ? optionsItem.getDisabledIconId() : optionsItem.getActiveIconId())));
						updateMenu();
					}
				})
				.create();
		return muteSoundItem[0];
	}

	private BaseBottomSheetItem createShowAlongTheRouteItem(final LocalRoutingParameter optionsItem) {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon((optionsItem.getActiveIconId())))
				.setTitle(getString(R.string.show_along_the_route))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
						FragmentManager fm = getFragmentManager();
						if (fm == null) {
							return;
						}
						Bundle args = new Bundle();
						ShowAlongTheRouteBottomSheet fragment = new ShowAlongTheRouteBottomSheet();
						fragment.setUsedOnMap(false);
						fragment.setArguments(args);
						fragment.setTargetFragment(RouteOptionsBottomSheet.this, ShowAlongTheRouteBottomSheet.REQUEST_CODE);
						fragment.show(fm, ShowAlongTheRouteBottomSheet.TAG);
						updateMenu();
					}
				}).create();
	}

	private BaseBottomSheetItem createRouteSimulationItem(final LocalRoutingParameter optionsItem) {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_start_navigation))
				.setTitle(getString(R.string.simulate_navigation))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						final OsmAndLocationProvider loc = app.getLocationProvider();
						loc.getLocationSimulation().startStopRouteAnimation(getActivity());
						dismiss();
					}
				})
				.create();
	}

	private BaseBottomSheetItem createAvoidRoadsTypesItem(final LocalRoutingParameter optionsItem) {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon((optionsItem.getActiveIconId())))
				.setTitle(getString(R.string.impassable_road))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
						AvoidRoadsBottomSheetDialogFragment avoidRoadsFragment = new AvoidRoadsBottomSheetDialogFragment();
						avoidRoadsFragment.setTargetFragment(RouteOptionsBottomSheet.this, AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE);
						avoidRoadsFragment.show(mapActivity.getSupportFragmentManager(), AvoidRoadsBottomSheetDialogFragment.TAG);
						updateMenu();
					}
				})
				.create();
	}

	private BaseBottomSheetItem createAvoidRoadsItem(final LocalRoutingParameter optionsItem) {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon((optionsItem.getActiveIconId())))
				.setTitle(getString(R.string.impassable_road))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
						AvoidRoadsBottomSheetDialogFragment avoidRoadsFragment = new AvoidRoadsBottomSheetDialogFragment();
						avoidRoadsFragment.setTargetFragment(RouteOptionsBottomSheet.this, AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE);
						avoidRoadsFragment.show(mapActivity.getSupportFragmentManager(), AvoidRoadsBottomSheetDialogFragment.TAG);
						updateMenu();
					}
				})
				.create();
	}

	private BaseBottomSheetItem createGpxRoutingItem(final LocalRoutingParameter optionsItem) {
		View view = mapActivity.getLayoutInflater().inflate(R.layout.plan_route_gpx, null);
		AndroidUtils.setTextPrimaryColor(mapActivity, (TextView) view.findViewById(R.id.title), nightMode);
		final TextView gpxDescription = (TextView) view.findViewById(R.id.description);

		((ImageView) view.findViewById(R.id.icon)).setImageDrawable(getContentIcon(optionsItem.getActiveIconId()));
		((ImageView) view.findViewById(R.id.dropDownIcon)).setImageDrawable(getContentIcon(R.drawable.ic_action_arrow_drop_down));

		RouteProvider.GPXRouteParamsBuilder rp = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		String gpxName;
		if (rp == null) {
			AndroidUtils.setTextSecondaryColor(mapActivity, gpxDescription, nightMode);
			gpxName = mapActivity.getString(R.string.choose_track_file_to_follow);
		} else {
			gpxDescription.setTextColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light));
			gpxName = new File(rp.getFile().path).getName();
		}
		gpxDescription.setText(gpxName);

		return new BaseBottomSheetItem.Builder().setCustomView(view).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showOptionsMenu(gpxDescription);
			}
		}).create();
	}

	private BaseBottomSheetItem createOtherSettingsRoutingItem(final LocalRoutingParameter optionsItem) {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(optionsItem.getActiveIconId()))
				.setTitle(getString(R.string.routing_settings_2))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						final Intent settings = new Intent(mapActivity, SettingsNavigationActivity.class);
						settings.putExtra(SettingsNavigationActivity.INTENT_SKIP_DIALOG, true);
						settings.putExtra(SettingsBaseActivity.INTENT_APP_MODE, routingHelper.getAppMode().getStringKey());
						mapActivity.startActivity(settings);
					}
				})
				.create();
	}

	private void inflateRoutingParameter(final LocalRoutingParameter parameter) {
		if (parameter != null) {
			final BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			BottomSheetItemWithCompoundButton.Builder builder = new BottomSheetItemWithCompoundButton.Builder();
			if (parameter.routingParameter != null || parameter instanceof RoutingOptionsHelper.OtherLocalRoutingParameter) {
				builder.setTitle(parameter.getText(mapActivity));
				int iconId = parameter.isSelected(settings) ? parameter.getActiveIconId() : parameter.getDisabledIconId();
				if (iconId != -1) {
					builder.setIcon(getContentIcon(iconId));
				}
			}
			if (parameter instanceof LocalRoutingParameterGroup) {
				final LocalRoutingParameterGroup group = (LocalRoutingParameterGroup) parameter;
				LocalRoutingParameter selected = group.getSelected(settings);
				if (selected != null) {
					builder.setTitle(group.getText(mapActivity));
					builder.setDescription(selected.getText(mapActivity));
				}
				builder.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp);
				builder.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, parameter);
						routingOptionsHelper.showLocalRoutingParameterGroupDialog(group, mapActivity, new RoutingOptionsHelper.OnClickListener() {
							@Override
							public void onClick() {
								LocalRoutingParameter selected = group.getSelected(settings);
								if (selected != null) {
									item[0].setDescription(selected.getText(mapActivity));
								}
								updateMenu();
							}
						});
					}
				});
			} else {
				builder.setLayoutId(R.layout.bottom_sheet_item_with_switch_56dp);
				if (parameter.routingParameter != null && parameter.routingParameter.getId().equals(GeneralRouter.USE_SHORTEST_WAY)) {
					// if short route settings - it should be inverse of fast_route_mode
					builder.setChecked(!settings.FAST_ROUTE_MODE.getModeValue(routingHelper.getAppMode()));
				} else {
					builder.setChecked(parameter.isSelected(settings));
				}
				builder.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, parameter);
						boolean selected = !parameter.isSelected(settings);
						routingOptionsHelper.applyRoutingParameter(parameter, selected);
						item[0].setChecked(selected);
						int iconId = selected ? parameter.getActiveIconId() : parameter.getDisabledIconId();
						if (iconId != -1) {
							item[0].setIcon(getContentIcon(iconId));
						}
						updateMenu();
					}
				});
			}
			item[0] = builder.create();
			items.add(item[0]);
		}
	}

	private void updateMenu() {
		final MapRouteInfoMenu mapRouteInfoMenu = getMapActivity().getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
		if (mapRouteInfoMenu != null) {
			mapRouteInfoMenu.updateMenu();
		}
	}

	private List<LocalRoutingParameter> getRoutingParameters(ApplicationMode applicationMode) {
		List<String> routingParameters = new ArrayList<>();
		if (applicationMode.equals(ApplicationMode.CAR)) {
			routingParameters = AppModeOptions.CAR.routingParameters;
		} else if (applicationMode.equals(ApplicationMode.BICYCLE)) {
			routingParameters = AppModeOptions.BICYCLE.routingParameters;
		} else if (applicationMode.equals(ApplicationMode.PEDESTRIAN)) {
			routingParameters = AppModeOptions.PEDESTRIAN.routingParameters;
		}

		return routingOptionsHelper.getRoutingParameters(applicationMode, routingParameters);
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(FragmentManager fragmentManager) {
		RouteOptionsBottomSheet f = new RouteOptionsBottomSheet();
		f.show(fragmentManager, RouteOptionsBottomSheet.TAG);
	}

	protected void openGPXFileSelection() {
		GpxUiHelper.selectGPXFile(mapActivity, false, false, new CallbackWithObject<GPXUtilities.GPXFile[]>() {

			@Override
			public boolean processResult(GPXUtilities.GPXFile[] result) {
				mapActivity.getMapActions().setGPXRouteParams(result[0]);
				app.getTargetPointsHelper().updateRouteAndRefresh(true);
				updateParameters();
				routingHelper.recalculateRouteDueToSettingsChange();
				return true;
			}
		});
	}

	private void showOptionsMenu(final TextView gpxSpinner) {
		RouteProvider.GPXRouteParamsBuilder rp = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		final PopupMenu optionsMenu = new PopupMenu(gpxSpinner.getContext(), gpxSpinner);
		MenuItem item = optionsMenu.getMenu().add(
				mapActivity.getString(R.string.shared_string_none));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (mapActivity.getRoutingHelper().getCurrentGPXRoute() != null) {
					mapActivity.getRoutingHelper().setGpxParams(null);
					settings.FOLLOW_THE_GPX_ROUTE.set(null);
					mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
				}
				updateParameters();
				return true;
			}
		});
		item = optionsMenu.getMenu().add(mapActivity.getString(R.string.select_gpx));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				openGPXFileSelection();
				return true;
			}
		});
		if (rp != null) {
			item = optionsMenu.getMenu().add(new File(rp.getFile().path).getName());
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					// nothing to change
					return true;
				}
			});
		}
		optionsMenu.show();
	}

	private void updateParameters() {
		View mainView = getView();
		if (mainView != null) {
			LinearLayout itemsContainer = (LinearLayout) mainView.findViewById(useScrollableItemsContainer()
					? R.id.scrollable_items_container : R.id.non_scrollable_items_container);
			if (itemsContainer != null) {
				itemsContainer.removeAllViews();
			}
			items.clear();
			createMenuItems(null);
			for (BaseBottomSheetItem item : items) {
				item.inflate(app, itemsContainer, nightMode);
			}
			setupHeightAndBackground(mainView);
		}
	}

	public enum AppModeOptions {

		CAR(MuteSoundRoutingParameter.KEY,
				DividerItem.KEY,
				AvoidRoadsRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				GeneralRouter.ALLOW_PRIVATE,
				GeneralRouter.USE_SHORTEST_WAY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		BICYCLE(MuteSoundRoutingParameter.KEY,
				DRIVING_STYLE,
				GeneralRouter.USE_HEIGHT_OBSTACLES,
				DividerItem.KEY,
				GeneralRouter.ALLOW_MOTORWAYS,
				AvoidRoadsTypesRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		PEDESTRIAN(MuteSoundRoutingParameter.KEY,
				GeneralRouter.USE_HEIGHT_OBSTACLES,
				DividerItem.KEY,
				AvoidRoadsTypesRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				RouteSimulationItem.KEY);


		List<String> routingParameters;

		AppModeOptions(String... routingParameters) {
			this.routingParameters = Arrays.asList(routingParameters);
		}
	}
}