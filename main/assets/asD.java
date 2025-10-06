package com.vitorxp.WorthClient.chat;

import com.vitorxp.WorthClient.config.ChatConfig;
import com.vitorxp.WorthClient.utils.RankUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatModifier {

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        // Ignora mensagens de sistema, invisibilidade, filas ou mensagens muito curtas.
        String formatted = event.message.getFormattedText();
        String raw = event.message.getUnformattedText();
        if (formatted.contains("Você está invisível para os outros jogadores!") || formatted.contains("Você está na Fila para entrar no servidor") || formatted.length() < 10 || event.message.getChatStyle() == null) {
            return;
        }

        // Ignora mensagens com caracteres especiais comuns em servidores para evitar duplicação de formatação.
        if (formatted.matches(".*[❤⚜✎❁✯⸕➣➤➥➫➳➵➸⏣⚔⛏].*")) {
            return;
        }

        IChatComponent modifiedComponent = event.message.createCopy();

        // 1. Adiciona o Timestamp (Horário) se estiver ativado na config
        if (ChatConfig.timestampEnabled) {
            // Usa o formato e a cor definidos na classe de configuração
            String format = ChatConfig.timestampFormat.replace("[", "").replace("]", "").trim();
            String hora = new SimpleDateFormat(format).format(new Date());

            IChatComponent timePrefix = new ChatComponentText(ChatConfig.timestampColor + "[" + hora + "] " + EnumChatFormatting.RESET);
            timePrefix.appendSibling(modifiedComponent);
            modifiedComponent = timePrefix;
        }

        // 2. Adiciona o Botão de Copiar se estiver ativado na config
        if (ChatConfig.copyButtonEnabled) {
            IChatComponent copyButton = new ChatComponentText(" " + EnumChatFormatting.DARK_AQUA + "[⎆]");
            String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String mensagemCompleta = "[" + hora + "] " + raw;

            copyButton.setChatStyle(new ChatStyle()
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/copiar_mensagem \"" + mensagemCompleta.replaceAll("\"", "'") + "\""))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Clique aqui para copiar esta mensagem.")))
            );
            modifiedComponent.appendSibling(copyButton);
        }

        // 3. Adiciona o Botão de Admin se estiver ativado e o jogador for Staff
        String remetente = extrairNome(raw);
        if (ChatConfig.adminButtonEnabled && RankUtils.isStaff(Minecraft.getMinecraft().thePlayer)) {
            if (!remetente.equalsIgnoreCase("Desconhecido")) {
                IChatComponent adminButton = new ChatComponentText(" " + EnumChatFormatting.RED + "[❈]");
                adminButton.setChatStyle(new ChatStyle()
                        .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/adminv " + remetente))
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Abrir painel de administração do player")))
                );
                modifiedComponent.appendSibling(adminButton);
            }
        }

        // 4. Toca o som de menção se estiver ativado
        Minecraft mc = Minecraft.getMinecraft();
        if (ChatConfig.mentionSound) {
            String playerName = mc.thePlayer.getName();
            if (raw.toLowerCase().contains(playerName.toLowerCase())) {
                mc.thePlayer.playSound("random.orb", 1.0F, 1.0F);
            }
        }

        // 5. A MUDANÇA MAIS IMPORTANTE:
        // Em vez de cancelar o evento e reimprimir a mensagem (o que quebraria o
        // chat infinito e a renderização customizada), nós simplesmente modificamos
        // a mensagem do evento e deixamos o jogo continuar seu fluxo normal.
        // Nosso CustomGuiNewChat vai pegar esta mensagem já modificada e renderizá-la.
        event.message = modifiedComponent;
    }

    private String extrairNome(String raw) {
        int idx = raw.indexOf(":");
        if (idx == -1) return "Desconhecido";

        String parteAntesDoTexto = raw.substring(0, idx).trim();
        String[] partes = parteAntesDoTexto.split(" ");
        if (partes.length < 1) return "Desconhecido";

        String nomePossivel = partes[partes.length - 1];

        // Otimização para evitar chamar o Minecraft.getMinecraft() múltiplas vezes
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equalsIgnoreCase(nomePossivel)) {
                    return nomePossivel;
                }
            }
        }

        return "Desconhecido";
    }
}
