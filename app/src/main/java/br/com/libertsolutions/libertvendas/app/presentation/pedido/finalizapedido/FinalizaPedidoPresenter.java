package br.com.libertsolutions.libertvendas.app.presentation.pedido.finalizapedido;

import br.com.libertsolutions.libertvendas.app.data.repository.Repository;
import br.com.libertsolutions.libertvendas.app.domain.pojo.FormaPagamento;
import br.com.libertsolutions.libertvendas.app.presentation.pedido.NavigateToNextEvent;
import br.com.libertsolutions.libertvendas.app.presentation.util.FormattingUtils;
import java.util.Calendar;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;

/**
 * @author Filipe Bezerra
 */
class FinalizaPedidoPresenter implements FinalizaPedidoContract.Presenter {
    private final FinalizaPedidoContract.View mView;

    private final Repository<FormaPagamento> mFormaPagamentoRepository;

    private final ProdutosSelecionadosArgumentExtractor mProdutosSelecionadosExtractor;

    private List<FormaPagamento> mFormaPagamentoList;

    private Calendar mDataEmissao = Calendar.getInstance();

    FinalizaPedidoPresenter(
            FinalizaPedidoContract.View pView,
            Repository<FormaPagamento> pFormaPagamentoRepository,
            ProdutosSelecionadosArgumentExtractor pProdutosSelecionadosExtractor) {
        mView = pView;
        mFormaPagamentoRepository = pFormaPagamentoRepository;
        mProdutosSelecionadosExtractor = pProdutosSelecionadosExtractor;
    }

    @Override public void initializeView() {
        mFormaPagamentoRepository
                .list()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        pFormaPagamentos -> {
                            mFormaPagamentoList = pFormaPagamentos;
                            mView.bindFormasPagamento(mFormaPagamentoList);
                        }
                );
    }

    @Override public void clickActionSave() {
        EventBus.getDefault().post(NavigateToNextEvent.notifyEvent());
    }

    @Override public void clickSelectCliente() {
        mView.navigateToListaClientesActivity();
    }

    @Override public void clickSelectDataEmissao() {
        mView.showCalendarPicker(mDataEmissao);
    }

    @Override public void setDataEmissao(int pYear, int pMonthOfYear, int pDayOfMonth) {
        mDataEmissao.set(Calendar.YEAR, pYear);
        mDataEmissao.set(Calendar.MONTH, pMonthOfYear);
        mDataEmissao.set(Calendar.DAY_OF_MONTH, pDayOfMonth);
        mView.bindDataEmissao(formatDataEmissao());
    }

    private String formatDataEmissao() {
        return FormattingUtils.convertMillisecondsToDateAsString(mDataEmissao.getTimeInMillis());
    }
}
