package br.com.libertsolutions.libertvendas.app.presentation.pedido.finalizapedido;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import br.com.libertsolutions.libertvendas.app.Injection;
import br.com.libertsolutions.libertvendas.app.R;
import br.com.libertsolutions.libertvendas.app.domain.pojo.FormaPagamento;
import br.com.libertsolutions.libertvendas.app.domain.vo.ProdutoVo;
import br.com.libertsolutions.libertvendas.app.presentation.fragment.LibertVendasFragment;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTouch;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import smtchahal.materialspinner.MaterialSpinner;

/**
 * @author Filipe Bezerra
 */
public class FinalizaPedidoFragment extends LibertVendasFragment
        implements FinalizaPedidoContract.View {

    public static final String ARG_PRODUTOS_SELECIONADOS
            = FinalizaPedidoFragment.class.getSimpleName() + ".argProdutosSelecionados";

    @BindView(R.id.edit_text_data_emissao) protected EditText mEditTextDataEmissao;
    @BindView(R.id.spinner_forma_pagamento) protected MaterialSpinner mSpinnerFormaPagamento;

    private FinalizaPedidoContract.Presenter mPresenter;

    private FormaPagamentoAdapter mFormaPagamentoAdapter;

    public static FinalizaPedidoFragment newInstance(List<ProdutoVo> pProdutosSelecionados) {
        FinalizaPedidoFragment fragment = new FinalizaPedidoFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelableArrayList(ARG_PRODUTOS_SELECIONADOS,
                new ArrayList<>(pProdutosSelecionados));
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPresenter = new FinalizaPedidoPresenter(this,
                Injection.provideFormaPagamentoRepository(getContext()),
                new ProdutosSelecionadosArgumentExtractor(getArguments()));
        setHasOptionsMenu(true);
    }

    @Override protected int provideContentViewResource() {
        return R.layout.fragment_finaliza_pedido;
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_finaliza_pedido, menu);
    }

    @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPresenter.initializeView();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            mPresenter.clickActionSave();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override public void bindFormasPagamento(List<FormaPagamento> pFormaPagamentoList) {
        mFormaPagamentoAdapter = new FormaPagamentoAdapter(getContext(), pFormaPagamentoList);
        mSpinnerFormaPagamento.setAdapter(mFormaPagamentoAdapter);
    }

    @Override public void navigateToListaClientesActivity() {
        hostActivity().navigate().toClientes();
    }

    @Override public void showCalendarPicker(Calendar pDataEmissao) {
        new CalendarDatePickerDialogFragment()
                .setOnDateSetListener((dialog, year, monthOfYear, dayOfMonth) -> {
                    mPresenter.setDataEmissao(year, monthOfYear, dayOfMonth);
                })
                .setPreselectedDate(
                        pDataEmissao.get(Calendar.YEAR),
                        pDataEmissao.get(Calendar.MONTH),
                        pDataEmissao.get(Calendar.DAY_OF_MONTH)
                )
                .setThemeCustom(R.style.Widget_Libert_BetterPickersDialogs)
                .show(getChildFragmentManager(), "DatePickerDialog");
    }

    @Override public void bindDataEmissao(String pDataEmissao) {
        mEditTextDataEmissao.setText(pDataEmissao);
    }

    @OnClick(R.id.edit_text_cliente) void onClickEditTextCliente() {
        mPresenter.clickSelectCliente();
    }

    @OnTouch(R.id.edit_text_data_emissao) boolean onTouchEditTextDataEmissao(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            mPresenter.clickSelectDataEmissao();
            return true;
        }
        return false;
    }
}
