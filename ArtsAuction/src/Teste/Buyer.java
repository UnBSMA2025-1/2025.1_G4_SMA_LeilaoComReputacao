package Teste;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.UUID;
import java.util.Random; 

/**
 * Classe que representa um agente comprador em um sistema de leilão de arte.
 * O comprador busca por uma obra de arte específica dentro de um orçamento máximo.
 */
public class Buyer extends Agent {
    // Atributos do comprador
    private String targetArtwork;       // Obra de arte desejada
    private int maxBudget;              // Orçamento máximo disponível
    private int availableBudget;        // Orçamento atual (pode diminuir com taxas de consulta)
    private AID consultantAID;          // Identificador do agente consultor
    private AID currentSeller;          // Identificador do vendedor atual
    private int currentPrice;           // Preço atual da obra
    private int suggestedPrice;         // Preço sugerido (pelo consultor ou calculado)
    private String conversationId;      // ID único para conversas/transações
    private boolean consulted = false;  // Flag indicando se já consultou especialista
    private int controller=0;           // Controle para lógica alternativa
    private Random random = new Random(); // Gerador de números aleatórios
    private double primeirolance = 0;   // Armazena o primeiro lance sugerido
  
    /**
     * Método de inicialização do agente comprador.
     * Configura os parâmetros iniciais e registra o serviço no DF (Directory Facilitator).
     */
    @Override
    protected void setup() {
        System.out.println("[BUYER] " + getAID().getLocalName() + " iniciando...");
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            // Obtém os argumentos passados na criação do agente
            targetArtwork = (String) args[0];
            maxBudget = Integer.parseInt((String) args[1]);
            availableBudget = maxBudget;
            System.out.println("[BUYER] Procurando por: " + targetArtwork + " | Orçamento total: $" + maxBudget);
            
            // Registra o serviço do comprador no DF
            registerBuyerService();
            // Inicia comportamento de busca por vendedores
            addBehaviour(new FindSellerBehaviour());
        } else {
            System.err.println("[BUYER] Argumentos inválidos");
            doDelete(); // Encerra o agente se não tiver argumentos suficientes
        }
    }

    /**
     * Método executado antes do agente ser encerrado.
     * Realiza o desregistro do serviço no DF.
     */
    @Override
    protected void takeDown() {
        // Desregistra do DF (Directory Facilitator)
        try {
            DFService.deregister(this);
            System.out.println("[BUYER] " + getAID().getLocalName() + " derregistrado do DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
        
    /**
     * Registra o serviço do comprador no DF (Directory Facilitator).
     */
    private void registerBuyerService() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(ArtConstants.SERVICE_TYPE_BUYER);
            sd.setName(ArtConstants.SERVICE_NAME_TRADING);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("Registrado no DF como comprador");
        } catch (FIPAException fe) {
            System.err.println("Falha ao registrar: " + fe.getMessage());
        }
    }

    /**
     * Comportamento que busca por vendedores registrados no DF.
     */
    private class FindSellerBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println("Procurando por vendedor...");
            try {
                // Cria template para busca de vendedores
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(ArtConstants.SERVICE_TYPE_SELLING);
                template.addServices(sd);
                
                // Realiza a busca no DF
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    // Armazena o primeiro vendedor encontrado
                    currentSeller = result[0].getName();
                    System.out.println("Vendedor encontrado: " + currentSeller.getLocalName());
                    // Adiciona comportamento para buscar consultor
                    addBehaviour(new FindConsultantBehaviour());
                } else {
                    System.out.println("Nenhum vendedor encontrado");
                }
            } catch (FIPAException fe) {
                System.err.println("Erro na busca: " + fe.getMessage());
            }
        }
    }

    /**
     * Comportamento que busca por consultores especialistas registrados no DF.
     */
    private class FindConsultantBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println("Procurando por consultor...");
            try {
                // Cria template para busca de consultores
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(ArtConstants.SERVICE_TYPE_CONSULTANT);
                template.addServices(sd);
                
                // Realiza a busca no DF
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    // Armazena o primeiro consultor encontrado
                    consultantAID = result[0].getName();
                    System.out.println("Consultor encontrado: " + consultantAID.getLocalName());
                } else {
                    System.out.println("Nenhum consultor encontrado");
                }
            } catch (FIPAException fe) {
                System.err.println("Erro na busca: " + fe.getMessage());
            }
            // Adiciona comportamento para lidar com CFPs (Call For Proposals)
            addBehaviour(new HandleCFPBehaviour());
        }
    }

    /**
     * Comportamento cíclico para lidar com mensagens CFP (Call For Proposals) de vendedores.
     */
    private class HandleCFPBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Cria template para filtrar mensagens CFP
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchOntology(ArtConstants.ONTOLOGY_ART_AUCTION)
            );
            
            // Recebe a mensagem (não bloqueante)
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                // Extrai informações da mensagem
                String[] contentParts = msg.getContent().split(";");
                String artwork = contentParts[0];
                currentPrice = Integer.parseInt(contentParts[1]);
                
                // Verifica se é a obra de arte desejada
                if (artwork.equals(targetArtwork)) {
                    currentSeller = msg.getSender();
                    
                    // Se ainda não consultou especialista, faz a consulta
                    if (!consulted) {
                        conversationId = UUID.randomUUID().toString();
                        addBehaviour(new ConsultExpertBehaviour());
                    } else {
                        // Caso contrário, faz uma oferta diretamente
                        makeOffer();
                    }
                }
            } else {
                block(); // Bloqueia até receber nova mensagem
            }
        }
        
        /**
         * Método para fazer uma oferta ao vendedor.
         */
        private void makeOffer() {
            // Calcula limite máximo para lance baseado no primeiro lance sugerido
            double limitBid = primeirolance *1.5;
            if(limitBid < suggestedPrice) {
                System.out.println("Valor muito Alto, Desistindo...");
                doDelete(); // Encerra o agente se o preço estiver muito alto
                return;
            }
            
            // Garante que a oferta não ultrapasse o orçamento disponível
            suggestedPrice = Math.min(suggestedPrice, availableBudget);
            
            System.out.println("Fazendo oferta de $" + suggestedPrice);
            
            // Prepara mensagem de proposta
            ACLMessage propose = new ACLMessage(ACLMessage.PROPOSE);
            propose.addReceiver(currentSeller);
            propose.setContent(targetArtwork + ";" + suggestedPrice);
            propose.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
            
            send(propose); // Envia a proposta
            
            // Configura template para esperar resposta do vendedor
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchSender(currentSeller),
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
            );
            
            // Aguarda resposta com timeout de 5 segundos
            ACLMessage response = blockingReceive(mt, 5000);
            
            if (response != null) {
                // Oferta aceita - compra realizada
                System.out.println("Oferta aceita! Compra realizada por $" + suggestedPrice);
                doDelete(); // Encerra o agente
            } else {
                // Oferta recusada ou timeout - aumenta o lance
                double incrementFator = 1 + random.nextDouble() * 0.2;
                suggestedPrice = Math.min((int)(suggestedPrice * incrementFator), availableBudget);
            }
        }
        
        /**
         * Comportamento para consultar um especialista sobre o preço da obra.
         */
        private class ConsultExpertBehaviour extends OneShotBehaviour {
            @Override
            public void action(){
                // Verifica se há consultor disponível e orçamento para taxa
                if (consultantAID != null && availableBudget >= ArtConstants.CONSULTATION_FEE) {
                    System.out.println("Pagando taxa de consulta de $" + ArtConstants.CONSULTATION_FEE);
                    availableBudget -= ArtConstants.CONSULTATION_FEE;
                    System.out.println("Orçamento após taxa: $" + availableBudget);
                    
                    System.out.println("Consultando especialista...");
                    // Prepara mensagem de requisição ao consultor
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(consultantAID);
                    request.setContent(targetArtwork + ";" + currentPrice + ";" + availableBudget);
                    request.setConversationId(conversationId);
                    request.setOntology(ArtConstants.ONTOLOGY_ART_AUCTION);
                    send(request);
                    
                    // Aguarda resposta do consultor com timeout
                    ACLMessage reply = blockingReceive(
                        MessageTemplate.and(
                            MessageTemplate.MatchConversationId(conversationId),
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                        ), 
                        ArtConstants.CONSULTANT_TIMEOUT
                    );
                    
                    if (reply != null) {
                        // Processa resposta do consultor
                        String[] parts = reply.getContent().split(";");
                        suggestedPrice = Integer.parseInt(parts[0]);
                        System.out.println("Sugestão recebida: $" + suggestedPrice);
                        consulted = true;
                        primeirolance = suggestedPrice; // Armazena o primeiro lance
                    } else {
                        // Timeout na consulta - usa preço atual como base
                        System.out.println("Timeout na consulta ao especialista");
                        suggestedPrice = currentPrice;
                    }
                    
                } else if (consultantAID == null && controller != 1) {
                    // Sem consultor disponível - usa lógica alternativa
                    System.out.println("Nenhum consultor disponível, voou usar meus conhecimentos");
                    double variacaoprilan = 0.70 + (0.8 - 0.7)*random.nextDouble();
                    suggestedPrice = (int)(currentPrice * variacaoprilan);
                    controller++;
                    makeOffer(); // Faz oferta imediatamente
                } else {
                    // Orçamento insuficiente para consulta
                    System.out.println("Orçamento insuficiente para consulta (necessário $" + 
                                     ArtConstants.CONSULTATION_FEE + ")");
                }
                makeOffer(); // Faz oferta após consulta (ou alternativa)
            }
        }
    }
}