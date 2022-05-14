import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ru.tinkoff.piapi.core.utils.DateUtils.timestampToString;
import static ru.tinkoff.piapi.core.utils.MapperUtils.moneyValueToBigDecimal;
import static ru.tinkoff.piapi.core.utils.MapperUtils.quotationToBigDecimal;


public class Main {
    static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        var token = args[0];
        var api = InvestApi.create(token);
        //операция [candles, order, stop]
        var operation = args[1];
        //Figi инструмента, который будет использоваться в методах исполнения заявок и стоп-заявок
        //Для тестирования рекомендуется использовать дешевые бумаги
        System.out.println(operation);
        if (args.length>2) {
            var figi = args[2];
            System.out.println("Figi="+figi);
            if (operation.equalsIgnoreCase("order")) {
                System.out.println("Order start..");
                var figas = Bot.randomFigi(api, 5);
                figi = figas.get(0);
                //System.out.println();
                System.out.println("Figi="+figi);
                Bot.ordersServiceExample(api, figi);
                System.out.println("Order done..");
            } else if (operation.equalsIgnoreCase("stop")) {
                Bot.stopOrdersServiceExample(api, figi);
            } else if (operation.equalsIgnoreCase("candles")) {
                Bot.getCandles(api, figi);
            }
        } else {
            if (operation.equalsIgnoreCase("instruments")){
                //System.out.println(operation);
                Bot.instrumentsServiceExample(api);
            } else if (operation.equalsIgnoreCase("user")){
                Bot.usersServiceExample(api);
            }
        }
    }

}
