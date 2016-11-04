package br.com.libertsolutions.libertvendas.app.presentation.importacao;

import br.com.libertsolutions.libertvendas.app.data.cidades.CidadeService;
import br.com.libertsolutions.libertvendas.app.data.formaspagamento.FormaPagamentoService;
import br.com.libertsolutions.libertvendas.app.data.repository.Repository;
import br.com.libertsolutions.libertvendas.app.domain.factory.CidadeFactory;
import br.com.libertsolutions.libertvendas.app.domain.factory.FormaPagamentoFactory;
import br.com.libertsolutions.libertvendas.app.domain.pojo.Cidade;
import br.com.libertsolutions.libertvendas.app.domain.pojo.FormaPagamento;
import java.io.IOException;
import java.util.List;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

/**
 * @author Filipe Bezerra
 */
class ImportacaoPresenter implements ImportacaoContract.Presenter {
    private final ImportacaoContract.View mView;

    private final FormaPagamentoService mFormaPagamentoService;

    private final Repository<FormaPagamento> mFormaPagamentoRepository;

    private final CidadeService mCidadeService;

    private final Repository<Cidade> mCidadeRepository;

    private boolean mIsDoingInitialDataSync = false;

    private Throwable mErrorMakingNetworkCall;

    ImportacaoPresenter(
            ImportacaoContract.View pView,
            FormaPagamentoService pFormaPagamentoService,
            Repository<FormaPagamento> pFormaPagamentoRepository,
            CidadeService pCidadeService,
            Repository<Cidade> pCidadeRepository) {
        mView = pView;
        mFormaPagamentoService = pFormaPagamentoService;
        mFormaPagamentoRepository = pFormaPagamentoRepository;
        mCidadeService = pCidadeService;
        mCidadeRepository = pCidadeRepository;
    }

    @Override public void startSync(boolean deviceConnected) {
        if (deviceConnected) {
            mView.showLoading();
            requestImportacao();
        } else {
            mView.showDeviceNotConnectedError();
        }
    }

    private void requestImportacao() {
        mIsDoingInitialDataSync = true;

        Observable<List<FormaPagamento>> getFormasPagamento = mFormaPagamentoService
                .get("18285835000109")
                .filter(list -> !list.isEmpty())
                .flatMap(data -> mFormaPagamentoRepository
                        .saveAll(FormaPagamentoFactory.createListFormaPagamento(data)));

        Observable<List<Cidade>> getCidades = mCidadeService
                .get()
                .filter(list -> !list.isEmpty())
                .flatMap(data -> mCidadeRepository.saveAll(CidadeFactory.createListCidade(data)));

        Observable
                .merge(
                        getFormasPagamento, getCidades)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        pResult -> {
                            mView.hideLoadingWithSuccess();
                        },

                        e -> {
                            Timber.e(e);
                            mErrorMakingNetworkCall = e;
                            mView.hideLoadingWithFail();
                        }
                );
    }

    @Override public void handleClickDoneMenuItem() {
        if (mIsDoingInitialDataSync) {
            mView.navigateToMainActivity();
        } else {
            mView.finishActivity();
        }
    }

    @Override public boolean isSyncDone() {
        return true;
        //return mDataSyncRepository.isInitialDataSynced();
    }

    @Override public void handleCancelOnSyncError() {
        mView.finishActivity();
    }

    @Override public void handleAnimationEnd(boolean success) {
        if (success) {
            mView.showSuccessMessage();
            mView.invalidateMenu();
        } else {
            showError();
        }
    }

    private void showError() {
        if (mErrorMakingNetworkCall instanceof HttpException) {
            mView.showServerError();
        } else if (mErrorMakingNetworkCall instanceof IOException) {
            mView.showNetworkError();
        } else {
            mView.showUnknownError();
        }
    }
}