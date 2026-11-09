//+------------------------------------------------------------------+
//| TradixBridge.mq4 - sends CLOSED orders from MetaTrader 4 straight |
//| to the TRADIX app on your phone over your own Wi-Fi (no cloud).   |
//| Setup: see TradixBridge.mq5. MT4: Tools > Options > Expert        |
//| Advisors > Allow WebRequest for listed URL.                       |
//+------------------------------------------------------------------+
#property strict
#property version "1.00"

extern string PhoneUrl        = "http://192.168.1.20:8765/deals"; // shown inside TRADIX
extern string Token           = "CHANGE_ME";                      // shown inside TRADIX
extern int    IntervalSeconds = 15;
extern int    DaysBack        = 30;

datetime lastSent = 0;
string   gvKey;

string Esc(string s)
  {
   StringReplace(s, "\\", "\\\\");
   StringReplace(s, "\"", "\\\"");
   return s;
  }

int OnInit()
  {
   gvKey = "TradixLastSent_" + IntegerToString(AccountNumber());
   if(GlobalVariableCheck(gvKey))
      lastSent = (datetime)(long)GlobalVariableGet(gvKey);
   EventSetTimer(MathMax(5, IntervalSeconds));
   Print("TradixBridge started. Target: ", PhoneUrl);
   return(INIT_SUCCEEDED);
  }

void OnDeinit(const int reason) { EventKillTimer(); }

void OnTimer()
  {
   datetime from = (lastSent > 0) ? lastSent : TimeCurrent() - DaysBack * 86400;
   string deals = "";
   int n = 0;
   datetime maxT = lastSent;

   for(int i = OrdersHistoryTotal() - 1; i >= 0; i--)
     {
      if(!OrderSelect(i, SELECT_BY_POS, MODE_HISTORY)) continue;
      int type = OrderType();
      if(type != OP_BUY && type != OP_SELL) continue;
      datetime ct = OrderCloseTime();
      if(ct <= from) continue;
      if(ct > maxT) maxT = ct;
      string item = "{\"ticket\":" + IntegerToString(OrderTicket()) +
                    ",\"symbol\":\"" + Esc(OrderSymbol()) + "\"" +
                    ",\"type\":\"" + (type == OP_BUY ? "buy" : "sell") + "\"" +
                    ",\"volume\":" + DoubleToString(OrderLots(), 2) +
                    ",\"profit\":" + DoubleToString(OrderProfit(), 2) +
                    ",\"commission\":" + DoubleToString(OrderCommission(), 2) +
                    ",\"swap\":" + DoubleToString(OrderSwap(), 2) +
                    ",\"openTime\":\"" + TimeToString(OrderOpenTime(), TIME_DATE | TIME_SECONDS) + "\"" +
                    ",\"time\":\"" + TimeToString(ct, TIME_DATE | TIME_SECONDS) + "\"}";
      if(n > 0) deals += ",";
      deals += item;
      n++;
     }
   if(n == 0) return;

   string json = "{\"token\":\"" + Esc(Token) + "\",\"platform\":\"MT4\",\"account\":" +
                 IntegerToString(AccountNumber()) + ",\"deals\":[" + deals + "]}";
   char post[]; char result[]; string resHeaders;
   int len = StringToCharArray(json, post, 0, StringLen(json), CP_UTF8);
   ArrayResize(post, len);
   int code = WebRequest("POST", PhoneUrl, "Content-Type: application/json\r\n", 5000, post, result, resHeaders);
   if(code == 200)
     {
      lastSent = maxT;
      GlobalVariableSet(gvKey, (double)(long)lastSent);
      Print("TradixBridge: sent ", n, " order(s).");
     }
   else
      Print("TradixBridge: send failed, HTTP ", code, " (error ", GetLastError(), ").");
  }
