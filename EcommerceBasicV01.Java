import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class EcommerceApplication {

    private RestTemplate restTemplate;

    public EcommerceApplication(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        return String.format("Hello %s!", name);
    }

    @GetMapping("/stock")
    public ResponseEntity<Stock> stock(@RequestParam(value = "symbol") String symbol) {
        Stock stock = YahooFinance.get(symbol);
        if (stock == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(stock, HttpStatus.OK);
    }

    @GetMapping("/stocks")
    public ResponseEntity<List<Stock>> stocks(@RequestParam(value = "symbols") List<String> symbols) {
        List<Stock> stocks = new ArrayList<>();
        for (String symbol : symbols) {
            Stock stock = YahooFinance.get(symbol);
            if (stock == null) {
                continue;
            }
            stocks.add(stock);
        }
        if (stocks.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(stocks, HttpStatus.OK);
    }

    @GetMapping("/forex")
    public ResponseEntity<Double> forex(@RequestParam(value = "from") String from, @RequestParam(value = "to") String to) {
        String url = String.format("https://api.exchangerate-api.com/v4/latest/%s", from);
        String response = restTemplate.getForObject(url, String.class);
        JSONObject json = new JSONObject(response);
        Double rate = json.getJSONObject("rates").getDouble(to);
        if (rate == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(rate, HttpStatus.OK);
    }

    @GetMapping("/candles")
    public ResponseEntity<List<Candle>> candles(@RequestParam(value = "symbol") String symbol,
                                                 @RequestParam(value = "interval", defaultValue = "1d") String interval,
                                                 @RequestParam(value = "start", required = false) String start,
                                                 @RequestParam(value = "end", required = false) String end) {
        Stock stock = YahooFinance.get(symbol);
        if (stock == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Calendar calendar = Calendar.getInstance();
        if (start != null) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                calendar.setTime(dateFormat.parse(start));
            } catch (Exception e) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } else {
            calendar.add(Calendar.MONTH, -1);
        }
        Date startDate = calendar.getTime();
        calendar = Calendar.getInstance();
        if (end != null) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                calendar.setTime(dateFormat.parse(end));
            } catch (Exception e) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        @GetMapping("/technical")
public ResponseEntity<List<TechnicalIndicator>> technical(@RequestParam(value = "symbol") String symbol,
                                                           @RequestParam(value = "indicator", defaultValue = "sma") String indicator,
                                                           @RequestParam(value = "interval", defaultValue = "1d") String interval,
                                                           @RequestParam(value = "start", required = false) String start,
                                                           @RequestParam(value = "end", required = false) String end,
                                                           @RequestParam(value = "period", defaultValue = "50") int period) {
    Stock stock = YahooFinance.get(symbol);
    if (stock == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    Calendar calendar = Calendar.getInstance();
    if (start != null) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            calendar.setTime(dateFormat.parse(start));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    } else {
        calendar.add(Calendar.MONTH, -1);
    }
    Date startDate = calendar.getTime();
    calendar = Calendar.getInstance();
    if (end != null) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            calendar.setTime(dateFormat.parse(end));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    Date endDate = calendar.getTime();
    List<TechnicalIndicator> indicators = new ArrayList<>();
    if (interval.equals("1d")) {
        switch (indicator) {
            case "sma":
                for (int i = period - 1; i < stock.getHistory().size(); i++) {
                    List<BigDecimal> prices = stock.getHistory().subList(i - period + 1, i + 1).stream()
                            .map(h -> h.getClose())
                            .collect(Collectors.toList());
                    BigDecimal sma = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(prices.size()), 2, BigDecimal.ROUND_HALF_UP);
                    TechnicalIndicator smaIndicator = new TechnicalIndicator("SMA", sma, stock.getHistory().get(i).getDate());
                    indicators.add(smaIndicator);
                }
                break;
            case "ema":
                BigDecimal multiplier = BigDecimal.valueOf(2)
                        .divide(BigDecimal.valueOf(period + 1), 6, BigDecimal.ROUND_HALF_UP);
                BigDecimal ema = null;
                for (int i = period - 1; i < stock.getHistory().size(); i++) {
                    if (i == period - 1) {
                        List<BigDecimal> prices = stock.getHistory().subList(i - period + 1, i + 1).stream()
                                .map(h -> h.getClose())
                                .collect(Collectors.toList());
                        ema = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(prices.size()), 2, BigDecimal.ROUND_HALF_UP);
                    } else {
                        BigDecimal close = stock.getHistory().get(i).getClose();
                        ema = close.multiply(multiplier)
                                .add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
                    }
                    TechnicalIndicator emaIndicator = new TechnicalIndicator("EMA", ema, stock.getHistory().get(i).getDate());
                    indicators.add(emaIndicator);
                }
                break;
            default:
                // unsupported indicator
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    } else {
        // unsupported interval
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST

public TechnicalIndicator(String name, BigDecimal value, Date date) {
    this.name = name;
    this.value = value;
    this.date = date;
}

public String getName() {
    return name;
}

public void setName(String name) {
    this.name = name;
}

public BigDecimal getValue() {
    return value;
}

public void setValue(BigDecimal value) {
    this.value = value;
}

public Date getDate() {
    return date;
}

public void setDate(Date date) {
    this.date = date;
}
private FinancialDataService financialDataService;

@Autowired
public void setFinancialDataService(FinancialDataService financialDataService) {
    this.financialDataService = financialDataService;
}

@GetMapping("/{symbol}")
public ResponseEntity<List<TechnicalIndicator>> getIndicators(@PathVariable String symbol,
                                                              @RequestParam(required = false) String interval,
                                                              @RequestParam(required = false) String startDate,
                                                              @RequestParam(required = false) String endDate,
                                                              @RequestParam(required = false) List<String> indicators) {
    if (interval == null || interval.isEmpty()) {
        interval = "daily";
    }
    if (startDate == null || startDate.isEmpty()) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        startDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
    }
    if (endDate == null || endDate.isEmpty()) {
        endDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    List<TechnicalIndicator> indicators = financialDataService.calculateIndicators(symbol, interval, startDate, endDate, indicators);

    if (indicators.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(indicators, HttpStatus.OK);
}
