package br.com.libertsolutions.libertvendas.app.data.pricetable;

import br.com.libertsolutions.libertvendas.app.domain.pojo.PriceTable;
import java.util.List;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * @author Filipe Bezerra
 */
public interface PriceTableApi {

    @GET("api/tabela/get") Observable<List<PriceTable>> get(@Query("cnpj") String cnpj);
}
