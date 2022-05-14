import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.core.InvestApi;

public class Main {
    static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        var token = args[0];

        //var api = InvestApi.create(token);
        var api =InvestApi.createSandbox(token);
       // var res = api.getSandboxService().openAccountSync();
        //System.out.println(res);
        log.info("Sand box mode = {}", api.isSandboxMode());
        //операция [candles, order, stop]
        var operation = args[1];
        //Figi инструмента, который будет использоваться в методах исполнения заявок и стоп-заявок
        //Для тестирования рекомендуется использовать дешевые бумаги
        log.info("Operation = {}", operation);
        if (args.length>2) {
            var param2 = args[2];
            log.info("param2 = {}", param2);
            if (operation.equalsIgnoreCase("order")) {
                log.info("Order start..");
                var acc = args[3];
                var direction  = args[4];
                OrderDirection dir;
                if (direction.equalsIgnoreCase("buy")) {
                    dir = OrderDirection.ORDER_DIRECTION_BUY;
                } else if (direction.equalsIgnoreCase("sell")){
                    dir = OrderDirection.ORDER_DIRECTION_SELL;
                } else {
                    log.error("Direction order is {}", OrderDirection.UNRECOGNIZED.name());
                    return;
                }
                Bot.ordersService(api, param2, acc, dir);
                log.info("Order done..");
            } else if (operation.equalsIgnoreCase("stop")) {
                var orderId = param2;
                Bot.stopOrder(api, orderId);
            } else if (operation.equalsIgnoreCase("candles")) {
                Bot.getCandles(api, param2);
            } else if (operation.equalsIgnoreCase("delete-account")){
                api.getSandboxService().closeAccountSync(param2);
                //api.getSandboxService(). .closeAccount(param2);
                log.info("Delete account = {}", param2);
            } else if (operation.equalsIgnoreCase("list-orders")){
                Bot.listOrders(api, param2);
            }
        } else {
            if (operation.equalsIgnoreCase("instruments")){
                //System.out.println(operation);
                Bot.instrumentsService(api);
            } else if (operation.equalsIgnoreCase("user")){
                Bot.usersService(api);
            }  else if (operation.equalsIgnoreCase("new-account")){
                var acc = api.getSandboxService().openAccountSync();
                log.info("Account = {}", acc);
            }
        }
    }
}
