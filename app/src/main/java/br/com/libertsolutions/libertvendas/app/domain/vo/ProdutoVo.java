package br.com.libertsolutions.libertvendas.app.domain.vo;

import android.os.Parcel;
import android.os.Parcelable;
import br.com.libertsolutions.libertvendas.app.domain.pojo.Produto;

/**
 * @author Filipe Bezerra
 */
public class ProdutoVo implements Parcelable {
    private final Produto produto;

    private double totalProdutos = 0;

    private float quantidadeAdicionada = 0;

    public ProdutoVo(Produto pProduto) {
        produto = pProduto;
    }

    protected ProdutoVo(Parcel in) {
        produto = in.readParcelable(Produto.class.getClassLoader());
        totalProdutos = in.readDouble();
        quantidadeAdicionada = in.readFloat();
    }

    public static final Creator<ProdutoVo> CREATOR = new Creator<ProdutoVo>() {
        @Override public ProdutoVo createFromParcel(Parcel in) {
            return new ProdutoVo(in);
        }

        @Override public ProdutoVo[] newArray(int size) {
            return new ProdutoVo[size];
        }
    };

    public String getNome() {
        return produto.getDescricao();
    }

    public double getPreco() {
        return produto.getPrecoVenda();
    }

    public double getTotalProdutos() {
        return totalProdutos;
    }

    public float getQuantidadeAdicionada() {
        return quantidadeAdicionada;
    }

    public Produto getProduto() {
        return produto;
    }

    public synchronized void addQuantidade() {
        quantidadeAdicionada++;
        calcularTotalProdutos();
    }

    public synchronized void setQuantidade(float pQuantidade) {
        quantidadeAdicionada = pQuantidade;
        calcularTotalProdutos();
    }

    public synchronized boolean removeQuantidade() {
        if (quantidadeAdicionada > 0) {
            quantidadeAdicionada--;
            calcularTotalProdutos();
            return true;
        }
        return false;
    }

    private void calcularTotalProdutos() {
        totalProdutos = quantidadeAdicionada * produto.getPrecoVenda();
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel pOut, int pFlags) {
        pOut.writeParcelable(produto, pFlags);
        pOut.writeDouble(totalProdutos);
        pOut.writeFloat(quantidadeAdicionada);
    }
}
