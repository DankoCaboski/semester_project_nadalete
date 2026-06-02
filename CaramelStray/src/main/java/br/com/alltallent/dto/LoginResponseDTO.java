package br.com.alltallent.dto;

public record LoginResponseDTO(String token, Integer userId, String nomeCompleto) {}