package br.com.alltallent.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class PerfilRequestDTO {
    private String nome;
    private String descricao;
}
