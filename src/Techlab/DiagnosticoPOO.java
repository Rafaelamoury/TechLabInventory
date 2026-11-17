package Techlab;

import Techlab.negocio.AtivoTI;
import Techlab.negocio.Servidor;
import Techlab.negocio.EstacaoTrabalho;
import Techlab.negocio.DispositivoRede;
import java.time.LocalDateTime;
import java.util.List;

/** Teste isolado: nao conecta nem altera o banco existente. */
public class DiagnosticoPOO {
    public static void main(String[] args) {
        LocalDateTime agora = LocalDateTime.now();
        List<AtivoTI> ativos = List.of(
            new Servidor("SRV-001", "PowerEdge", "Ativo", agora, "192.168.0.10", "Linux", 32),
            new EstacaoTrabalho("EST-042", "ThinkPad", "Ativo", agora, "TI", "Operador"),
            new DispositivoRede("RED-003", "Switch 24 portas", "Ativo", agora, 24)
        );
        for (AtivoTI ativo : ativos) {
            System.out.printf("%s | custo mensal: R$ %.2f%n",
                    ativo, ativo.calcularCustoManutencaoMensal());
        }
        try {
            new DispositivoRede("", "Switch", "Ativo", agora, 24);
            throw new AssertionError("Validacao do patrimonio nao funcionou.");
        } catch (IllegalArgumentException esperado) {
            System.out.println("Validacao de patrimonio: OK");
        }
        System.out.println("TESTE POO CONCLUIDO COM SUCESSO");
    }
}
