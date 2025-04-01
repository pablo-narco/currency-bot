package uz.pdp;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CurrencyBot extends TelegramLongPollingBot {
    private Map<Long, Integer> lastMessageIds = new HashMap<>();

    @Override
    public String getBotUsername() {
        return "currency774_bot";
    }

    @Override
    public String getBotToken() {
        return "7062336384:AAGPQWLmOHorxCxvU5CzYBD5qDUfbNHMmgI";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    sendStartMessage(chatId);
                    break;
                case "💰 USD":
                    sendCurrencyRate(chatId, "USD");
                    break;
                case "🇪🇺 EUR":
                    sendCurrencyRate(chatId, "EUR");
                    break;
                case "🇷🇺 RUB":
                    sendCurrencyRate(chatId, "RUB");
                    break;
                case "🇬🇧 GBP":
                    sendCurrencyRate(chatId, "GBP");
                    break;
                case "🇨🇳 CNY":
                    sendCurrencyRate(chatId, "CNY");
                    break;
                case "🇰🇷 KRW":
                    sendCurrencyRate(chatId, "KRW");
                    break;
                default:
                    if (messageText.matches("\\d+(\\.\\d+)? (USD|EUR|RUB|GBP|CNY|KRW)")) {
                        calculateConversion(chatId, messageText);
                    } else {
                        sendMessageWithDelete(chatId, "Noto'g'ri buyruq. Iltimos, tugmalardan foydalaning.");
                    }
            }
        }
    }

    private void sendStartMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("💱 Valyuta Kurslari Botiga Xush Kelibsiz!\n" +
                "Quyidagi valyutalardan birini tanlang yoki miqdorni kiriting (masalan: 100 USD)");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("💰 USD"));
        row1.add(new KeyboardButton("🇪🇺 EUR"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🇷🇺 RUB"));
        row2.add(new KeyboardButton("🇬🇧 GBP"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🇨🇳 CNY"));
        row3.add(new KeyboardButton("🇰🇷 KRW"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        sendMessageWithDelete(chatId, message);
    }

    private void sendCurrencyRate(long chatId, String currencyCode) {
        double rate = getCurrencyRateFromCBU(currencyCode);

        String response = STR."""
\uD83D\uDD39 \{getCurrencyFullName(currencyCode)}
\uD83D\uDCCA Kurs: 1 \{currencyCode} = \{rate} so'm
ℹ\uFE0F \{getCurrencyDescription(currencyCode)}

\uD83D\uDCB1 Hisoblash uchun miqdorni kiriting:
Masalan: 100 \{currencyCode}""";

        sendMessageWithDelete(chatId, response, currencyCode);
    }

    private void calculateConversion(long chatId, String message) {
        String[] parts = message.split(" ");
        double amount = Double.parseDouble(parts[0]);
        String currency = parts[1];

        double rate = getCurrencyRateFromCBU(currency);
        double result = amount * rate;

        String response = String.format("💱 Konvertatsiya natijasi:\n" +
                        "%.2f %s = %.2f so'm\n\n" +
                        "📅 Kurs: 1 %s = %.2f so'm",
                amount, currency, result, currency, rate);

        sendMessageWithDelete(chatId, response, currency);
    }

    private double getCurrencyRateFromCBU(String currencyCode) {
        try {
            String url = STR."https://cbu.uz/uz/arkhiv-kursov-valyut/json/\{currencyCode}/";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(response.body()).getAsJsonArray().get(0).getAsJsonObject();

            return jsonObject.get("Rate").getAsDouble();
        } catch (Exception e) {
            e.printStackTrace();
            // Standart qiymatlar agar API ishlamasa
            return switch (currencyCode) {
                case "USD" -> 12500.0;
                case "EUR" -> 13500.0;
                case "RUB" -> 140.0;
                case "GBP" -> 15000.0;
                case "CNY" -> 1800.0;
                case "KRW" -> 9.5;
                default -> 1.0;
            };
        }
    }

    private String getCurrencyFullName(String currencyCode) {
        return switch (currencyCode) {
            case "USD" -> "AQSH dollari (USD)";
            case "EUR" -> "Yevro (EUR)";
            case "RUB" -> "Rus rubli (RUB)";
            case "GBP" -> "Funt sterling (GBP)";
            case "CNY" -> "Xitoy yuani (CNY)";
            case "KRW" -> "Koreya voni (KRW)";
            default -> currencyCode;
        };
    }

    private String getCurrencyDescription(String currencyCode) {
        return switch (currencyCode) {
            case "USD" -> "Amerika Qo'shma Shtatlari dollari";
            case "EUR" -> "Yevropa Ittifoqi rasmiy valyutasi";
            case "RUB" -> "Rossiya Federatsiyasi rasmiy valyutasi";
            case "GBP" -> "Buyuk Britaniya funt sterlingi";
            case "CNY" -> "Xitoy Xalq Respublikasi yuani";
            case "KRW" -> "Janubiy Koreya voni";
            default -> "";
        };
    }

    private void sendMessageWithDelete(long chatId, String text, String currencyCode) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();


        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("💰 USD"));
        row1.add(new KeyboardButton("🇪🇺 EUR"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🇷🇺 RUB"));
        row2.add(new KeyboardButton("🇬🇧 GBP"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🇨🇳 CNY"));
        row3.add(new KeyboardButton("🇰🇷 KRW"));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("100 " + currencyCode));
        row4.add(new KeyboardButton("500 " + currencyCode));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        sendMessageWithDelete(chatId, message);
    }

    private void sendMessageWithDelete(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        sendMessageWithDelete(chatId, message);
    }

    private void sendMessageWithDelete(long chatId, SendMessage message) {
        try {
            if (lastMessageIds.containsKey(chatId)) {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId);
                deleteMessage.setMessageId(lastMessageIds.get(chatId));
                execute(deleteMessage);
            }

            Message sentMessage = execute(message);

            lastMessageIds.put(chatId, sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}