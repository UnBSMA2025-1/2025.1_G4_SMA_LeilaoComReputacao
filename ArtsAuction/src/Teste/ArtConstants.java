package Teste;

/**
 * Classe de constantes utilizadas no sistema de leilão de arte.
 * Centraliza todas as strings e valores fixos usados no sistema multiagente.
 */
public class ArtConstants {
    /**
     * Tipo de serviço para agentes consultores especializados em arte.
     * Usado no DF (Directory Facilitator) para identificar serviços de consultoria.
     */
    public static final String SERVICE_TYPE_CONSULTANT = "art-consultant";

    /**
     * Tipo de serviço para agentes vendedores de obras de arte.
     * Usado no DF para identificar serviços de venda.
     */
    public static final String SERVICE_TYPE_SELLING = "art-selling";

    /**
     * Tipo de serviço para agentes compradores de obras de arte.
     * Usado no DF para identificar serviços de compra.
     */
    public static final String SERVICE_TYPE_BUYER = "art-buyer";

    /**
     * Nome do serviço de negociação de arte.
     * Usado no DF como nome comum para os serviços relacionados a transações de arte.
     */
    public static final String SERVICE_NAME_TRADING = "art-trading";

    /**
     * Ontologia usada nas mensagens ACL entre agentes.
     * Define o vocabulário comum para comunicação sobre leilões de arte.
     */
    public static final String ONTOLOGY_ART_AUCTION = "art-auction-ontology";

    /**
     * Timeout para consultas a especialistas (em milissegundos).
     * Tempo máximo que um agente aguardará por resposta de um consultor.
     */
    public static final long CONSULTANT_TIMEOUT = 15000; // 15 segundos

    /**
     * Taxa cobrada por consultoria especializada.
     * Valor fixo que os compradores pagam para obter assessoria sobre lances.
     */
    public static final int CONSULTATION_FEE = 25;
}