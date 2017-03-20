package br.com.libertsolutions.libertvendas.app.presentation.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import br.com.libertsolutions.libertvendas.app.R;
import br.com.libertsolutions.libertvendas.app.data.order.OrderRepository;
import br.com.libertsolutions.libertvendas.app.data.order.OrderedOrdersBySalesmanAndCompanySpecification;
import br.com.libertsolutions.libertvendas.app.domain.pojo.LoggedUser;
import br.com.libertsolutions.libertvendas.app.domain.pojo.Order;
import br.com.libertsolutions.libertvendas.app.domain.pojo.OrderChartData;
import br.com.libertsolutions.libertvendas.app.presentation.addorder.orderform.SavedOrderEvent;
import br.com.libertsolutions.libertvendas.app.presentation.base.BaseFragment;
import br.com.libertsolutions.libertvendas.app.presentation.main.LoggedInUserEvent;
import butterknife.BindView;
import butterknife.OnClick;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.greenrobot.eventbus.Subscribe;
import rx.Subscriber;
import rx.Subscription;
import timber.log.Timber;

import static br.com.libertsolutions.libertvendas.app.data.LocalDataInjector.providerOrderRepository;
import static br.com.libertsolutions.libertvendas.app.presentation.util.NumberUtils.withDefaultValue;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

/**
 * @author Filipe Bezerra
 */
public class DashboardFragment extends BaseFragment
        implements SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG = DashboardFragment.class.getName();

    private OrderRepository mOrderRepository;

    private Subscription mCurrentSubscription;

    private LoggedUser mLoggedUser;

    private OnGlobalLayoutListener mPieChartLayoutListener = null;

    @BindView(R.id.swipe_container_all_pull_refresh) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.pie_chart_orders_by_customer) PieChart mPieChart;

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Override protected int provideContentViewResource() {
        return R.layout.fragment_dashboard;
    }

    @Override public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable @Override public View onCreateView(final LayoutInflater inflater,
            @Nullable final ViewGroup container, @Nullable final Bundle inState) {
        View view = super.onCreateView(inflater, container, inState);

        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(android.R.color.white);

        drawPieChart();

        mOrderRepository = providerOrderRepository();
        eventBus().register(this);

        return view;
    }

    @Override public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.menu_filter, menu);
    }

    @Override public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.action_all_filter) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onRefresh() {
        loadOrderedOrders();
    }

    @Subscribe(sticky = true) public void onLoggedInUserEvent(LoggedInUserEvent event) {
        if (mLoggedUser == null || !mLoggedUser.equals(event.getUser())) {
            mLoggedUser = event.getUser();
            loadOrderedOrders();
        }
    }

    @Subscribe(sticky = true) public void onSavedOrder(SavedOrderEvent event) {
        loadOrderedOrders();
    }

    @OnClick(R.id.button_all_retry) void onButtonRetryClicked() {
        mLinearLayoutErrorState.setVisibility(View.GONE);
        loadOrderedOrders();
    }

    private void drawPieChart() {
        mPieChart.getDescription().setEnabled(false);

        //Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Light.ttf");

        //mPieChart.setCenterTextTypeface(tf);
        mPieChart.setCenterText(generateCenterText());
        mPieChart.setCenterTextSize(10f);
        //mPieChart.setCenterTextTypeface(tf);

        // radius of the center hole in percent of maximum radius
        mPieChart.setHoleRadius(45f);
        mPieChart.setTransparentCircleRadius(50f);

        Legend l = mPieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
    }

    private SpannableString generateCenterText() {
        SpannableString s = new SpannableString(getString(R.string.dashboard_chart_center_text));
        s.setSpan(new RelativeSizeSpan(2f), 0, 7, 0);
        s.setSpan(new ForegroundColorSpan(Color.GRAY), 8, s.length(), 0);
        return s;
    }

    private void loadOrderedOrders() {
        if (getLoggedUser() != null) {
            mCurrentSubscription = mOrderRepository
                    .query(new OrderedOrdersBySalesmanAndCompanySpecification(
                            getSalesmanId(), getCompanyId()))
                    .map(this::toChartData)
                    .observeOn(mainThread())
                    .doOnUnsubscribe(() -> mSwipeRefreshLayout.setRefreshing(false))
                    .subscribe(createOrderChartDataListSubscriber());
        }
    }

    private LoggedUser getLoggedUser() {
        if (mLoggedUser == null) {
            LoggedInUserEvent event = eventBus().getStickyEvent(LoggedInUserEvent.class);
            if (event != null) {
                mLoggedUser = event.getUser();
            }
        }
        return mLoggedUser;
    }

    private int getSalesmanId() {
        return getLoggedUser().getSalesman().getSalesmanId();
    }

    private int getCompanyId() {
        return getLoggedUser().getDefaultCompany().getCompanyId();
    }

    private List<OrderChartData> toChartData(List<Order> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<OrderChartData> chartData = new ArrayList<>();
        float orderAmount = 0;
        String name = orders.get(0).getCustomer().getName();
        for (Order order : orders) {
            if (!name.equals(order.getCustomer().getName())) {
                chartData.add(OrderChartData.create(name, orderAmount));
                name = order.getCustomer().getName();
                orderAmount = 0;
            }

            orderAmount += order.getTotalItems();
            orderAmount -= withDefaultValue(order.getDiscount(), 0);

            if (orders.indexOf(order) == orders.size() - 1) {
                chartData.add(OrderChartData.create(name, orderAmount));
            }
        }
        return chartData;
    }

    private Subscriber<List<OrderChartData>> createOrderChartDataListSubscriber() {
        return new Subscriber<List<OrderChartData>>() {
            @Override public void onStart() {
                startLoadingOrderedOrders();
            }

            @Override public void onError(final Throwable e) {
                handleLoadOrderedOrdersError(e);
            }

            @Override public void onNext(final List<OrderChartData> orders) {
                showOrderedOrders(orders);
            }

            @Override public void onCompleted() {}
        };
    }

    private void startLoadingOrderedOrders() {
        mSwipeRefreshLayout.setRefreshing(true);
        mPieChart.setVisibility(View.GONE);
        mLinearLayoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void handleLoadOrderedOrdersError(Throwable e) {
        Timber.e(e, "Could not chart data");
        mSwipeRefreshLayout.setRefreshing(false);
        mLinearLayoutErrorState.setVisibility(View.VISIBLE);
        mLinearLayoutEmptyState.setVisibility(View.GONE);
    }

    private void showOrderedOrders(List<OrderChartData> orders) {
        if (!orders.isEmpty()) {
            mPieChart.setVisibility(View.VISIBLE);
            mPieChart.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            mPieChartLayoutListener = this::onPieChartFinishLoading);
            convertToPieData(orders);
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    private void convertToPieData(List<OrderChartData> data) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        for(OrderChartData item : data) {
            entries.add(new PieEntry(item.getAmount(), item.getName()));
        }

        PieDataSet ds1 = new PieDataSet(entries, getString(R.string.dashboard_chart_label));
        ds1.setColors(ColorTemplate.MATERIAL_COLORS);
        ds1.setSliceSpace(2f);
        ds1.setValueTextColor(Color.WHITE);
        ds1.setValueTextSize(12f);

        //Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Regular.ttf");

        PieData pieData = new PieData(ds1);
        //pieData.setValueTypeface(tf);
        pieData.setValueFormatter(new DefaultValueFormatter(2));

        showChartData(pieData);
    }

    private void showChartData(PieData pieData) {
        mPieChart.setData(pieData);
        mPieChart.setDrawEntryLabels(false);
        mPieChart.invalidate();
    }

    private void onPieChartFinishLoading() {
        if (getView() != null) {
            mPieChart
                    .getViewTreeObserver()
                    .removeOnGlobalLayoutListener(mPieChartLayoutListener);
            mPieChartLayoutListener = null;
            mSwipeRefreshLayout.setRefreshing(false);
            mLinearLayoutEmptyState.setVisibility(View.GONE);
        }
    }

    @Override public void onDestroyView() {
        if (mCurrentSubscription != null && !mCurrentSubscription.isUnsubscribed()) {
            mCurrentSubscription.unsubscribe();
        }
        eventBus().unregister(this);
        super.onDestroyView();
    }
}
