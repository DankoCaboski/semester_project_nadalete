package br.com.alltallent.dto;

import br.com.alltallent.model.FuncionarioCertificado;

public record CertificadoDTO(Integer codigo, String nome) {
    public CertificadoDTO(FuncionarioCertificado certificado) {
        this(certificado.getCodigo(), certificado.getCertificado());
    }
}