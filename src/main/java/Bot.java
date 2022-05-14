import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToString;
import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;

public class Bot {
    static final Logger log = LoggerFactory.getLogger(Bot.class);

    public static void printCandle(HistoricCandle candle) {
        var open = quotationToBigDecimal(candle.getOpen());
        var close = quotationToBigDecimal(candle.getClose());
        var high = quotationToBigDecimal(candle.getHigh());
        var low = quotationToBigDecimal(candle.getLow());
        var volume = candle.getVolume();
        var time = timestampToString(candle.getTime());
        log.info(
                "цена открытия: {}, цена закрытия: {}, минимальная цена за 1 лот: {}, максимальная цена за 1 лот: {}, объем " +
                        "торгов в лотах: {}, время свечи: {}",
                open, close, low, high, volume, time);
    }

    public static void getCandles(InvestApi api, String figi) {

        //Получаем и печатаем список свечей для инструмента
        //var figi = randomFigi(api, 1).get(0);
        var candles1min = api.getMarketDataService()
                .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
                        CandleInterval.CANDLE_INTERVAL_1_MIN);
        var candles5min = api.getMarketDataService()
                .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
                        CandleInterval.CANDLE_INTERVAL_5_MIN);
        var candles15min = api.getMarketDataService()
                .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
                        CandleInterval.CANDLE_INTERVAL_15_MIN);
        var candlesHour = api.getMarketDataService()
                .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(),
                        CandleInterval.CANDLE_INTERVAL_HOUR);
        var candlesDay = api.getMarketDataService()
                .getCandlesSync(figi, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now(), CandleInterval.CANDLE_INTERVAL_DAY);

        log.info("получено {} 1-минутных свечей для инструмента с figi {}", candles1min.size(), figi);
        for (HistoricCandle candle : candles1min) {
            printCandle(candle);
        }

        log.info("получено {} 5-минутных свечей для инструмента с figi {}", candles5min.size(), figi);
        for (HistoricCandle candle : candles5min) {
            printCandle(candle);
        }

        log.info("получено {} 15-минутных свечей для инструмента с figi {}", candles15min.size(), figi);
        for (HistoricCandle candle : candles15min) {
            printCandle(candle);
        }

        log.info("получено {} 1-часовых свечей для инструмента с figi {}", candlesHour.size(), figi);
        for (HistoricCandle candle : candlesHour) {
            printCandle(candle);
        }

        log.info("получено {} 1-дневных свечей для инструмента с figi {}", candlesDay.size(), figi);
        for (HistoricCandle candle : candlesDay) {
            printCandle(candle);
        }
    }

    public static List<String> randomFigi(InvestApi api, int count) {
        return api.getInstrumentsService().getTradableSharesSync()
                .stream()
                .filter(el -> Boolean.TRUE.equals(el.getApiTradeAvailableFlag()))
                .map(Share::getFigi)
                .limit(count)
                .collect(Collectors.toList());
    }

    public static void stopOrdersServiceExample(InvestApi api, String figi) {

        //Выставляем стоп-заявку
        var accounts = api.getUserService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();

        var lastPrice = api.getMarketDataService().getLastPricesSync(List.of(figi)).get(0).getPrice();
        var minPriceIncrement = api.getInstrumentsService().getInstrumentByFigiSync(figi).getMinPriceIncrement();
        var stopPrice = Quotation.newBuilder().setUnits(lastPrice.getUnits() - minPriceIncrement.getUnits() * 100)
                .setNano(lastPrice.getNano() - minPriceIncrement.getNano() * 100).build();
        var stopOrderId = api.getStopOrdersService()
                .postStopOrderGoodTillDateSync(figi, 1, stopPrice, stopPrice, StopOrderDirection.STOP_ORDER_DIRECTION_BUY,
                        mainAccount, StopOrderType.STOP_ORDER_TYPE_STOP_LOSS, Instant.now().plus(1, ChronoUnit.DAYS));
        log.info("выставлена стоп-заявка. id: {}", stopOrderId);

        //Получаем список стоп-заявок и смотрим, что наша заявка в ней есть
        var stopOrders = api.getStopOrdersService().getStopOrdersSync(mainAccount);
        stopOrders.stream().filter(el -> el.getStopOrderId().equals(stopOrderId)).findAny().orElseThrow();

        //Отменяем созданную стоп-заявку
        api.getStopOrdersService().cancelStopOrder(mainAccount, stopOrderId);
        log.info("стоп заявка с id {} отменена", stopOrderId);
    }

    public static void ordersServiceExample(InvestApi api, String figi) {
        //Выставляем заявку
        var accounts = api.getUserService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();

        var lastPrice = api.getMarketDataService().getLastPricesSync(List.of(figi)).get(0).getPrice();
        var minPriceIncrement = api.getInstrumentsService().getInstrumentByFigiSync(figi).getMinPriceIncrement();
        var price = Quotation.newBuilder().setUnits(lastPrice.getUnits() - minPriceIncrement.getUnits() * 100)
                .setNano(lastPrice.getNano() - minPriceIncrement.getNano() * 100).build();

        //Выставляем заявку на покупку по лимитной цене
        var orderId = api.getOrdersService()
                .postOrderSync(figi, 1, price, OrderDirection.ORDER_DIRECTION_BUY, mainAccount, OrderType.ORDER_TYPE_LIMIT,
                        UUID.randomUUID().toString()).getOrderId();

        //Получаем список активных заявок, проверяем наличие нашей заявки в списке
        var orders = api.getOrdersService().getOrdersSync(mainAccount);
        if (orders.stream().anyMatch(el -> orderId.equals(el.getOrderId()))) {
            log.info("заявка с id {} есть в списке активных заявок", orderId);
        }

        //Отменяем заявку
        api.getOrdersService().cancelOrder(mainAccount, orderId);
    }

    public static void instrumentsServiceExample(InvestApi api) {
        var bonds = api.getInstrumentsService().getTradableBondsSync();
        var futures = api.getInstrumentsService().getTradableFuturesSync();
        log.info("Список Bonds");
        for (int i = 0; i < bonds.size(); i++) {
            var bond = bonds.get(i);
            var figi = bond.getFigi();
            log.info("figi {} code {} currency {}", figi, bond.getClassCode(), bond.getCurrency());
        }
        log.info("Список Futures");
        for (int i = 0; i < futures.size(); i++) {
            var future = futures.get(i);
            var figi = future.getFigi();
            log.info("figi {} code {} currency {}", figi, future.getClassCode(), future.getCurrency());
        }
    }

    public static void usersServiceExample(InvestApi api) {
        //Получаем список аккаунтов и распечатываем их с указанием привилегий токена
        var accounts = api.getUserService().getAccountsSync();
        var mainAccount = accounts.get(0);
        for (Account account : accounts) {
            log.info("account id: {}, access level: {}", account.getId(), account.getAccessLevel().name());
        }

        //Получаем и печатаем информацию о текущих лимитах пользователя
        var tariff = api.getUserService().getUserTariffSync();
        log.info("stream type: marketdata, stream limit: {}", tariff.getStreamLimitsList().get(0).getLimit());
        log.info("stream type: orders, stream limit: {}", tariff.getStreamLimitsList().get(1).getLimit());
        log.info("current unary limit per minute: {}", tariff.getUnaryLimitsList().get(0).getLimitPerMinute());

        //Получаем и печатаем информацию об обеспеченности портфеля
        var marginAttributes = api.getUserService().getMarginAttributesSync(mainAccount.getId());
        log.info("Ликвидная стоимость портфеля: {}", moneyValueToBigDecimal(marginAttributes.getLiquidPortfolio()));
        log.info("Начальная маржа — начальное обеспечение для совершения новой сделки: {}",
                moneyValueToBigDecimal(marginAttributes.getStartingMargin()));
        log.info("Минимальная маржа — это минимальное обеспечение для поддержания позиции, которую вы уже открыли: {}",
                moneyValueToBigDecimal(marginAttributes.getMinimalMargin()));
        log.info("Уровень достаточности средств. Соотношение стоимости ликвидного портфеля к начальной марже: {}",
                quotationToBigDecimal(marginAttributes.getFundsSufficiencyLevel()));
        log.info("Объем недостающих средств. Разница между стартовой маржой и ликвидной стоимости портфеля: {}",
                moneyValueToBigDecimal(marginAttributes.getAmountOfMissingFunds()));
    }
}
