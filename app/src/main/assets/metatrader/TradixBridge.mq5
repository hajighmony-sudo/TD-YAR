//+------------------------------------------------------------------+
//| TradixBridge.mq5 - sends CLOSED deals from MetaTrader 5 straight  |
//| to the TRADIX app on your phone over your own Wi-Fi (no cloud).   |
//|                                                                  |
//| Setup (once):                                                     |
//| 1. TRADIX > More > MetaTrader > "Direct bridge" > Start.         |
//| 2. MT5: Tools > Options > Expert Advisors > tick "Allow          |
//|    WebRequest for listed URL" and add the URL shown in TRADIX     |
//|    (e.g. http://192.168.1.20:8765).                               |
//| 3. Copy this file into MQL5/Experts, compile in MetaEditor,       |
//|    drag the EA on any chart, paste URL + token into the inputs.   |
//| Phone and PC must be on the same network.                         |
//+------------------------------------------------------------------+
#property strict
#property version "1.00"

input string PhoneUrl        = "http://192.168.1.20:8765/deals"; // shown inside TRADIX
input string Token           = "CHANGE_ME";                      // shown inside TRADIX
input int    IntervalSeconds = 15;                               // how often to look for new closed deals
input int    DaysBack        = 30;                               // first run: how many days of history to send

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
   gvKey = "TradixLastSent_" + (string)AccountInfoInteger(ACCOUNT_LOGIN);
   if(GlobalVariableCheck(gvKey))
      lastSent = (datetime)(long)GlobalVariableGet(gvKey);
   EventSetTimer(MathMax(5, IntervalSeconds));
   Print("TradixBridge started. Target: ", PhoneUrl);
   return(INIT_SUCCEEDED);
  }

void OnDeinit(const int reason) { EventKillTimer(); }

void OnTimer()
  {
   datetime from = (lastSent > 0) ? lastSent - 1 : TimeCurrent() - (datetime)(DaysBack * 86400);
   if(!HistorySelect(from, TimeCurrent() + 60))
      return;

   int total = HistoryDealsTotal();
   string deals = "";
   int n = 0;
   datetime maxT = lastSent;

   for(int i = 0; i < total; i++)
     {
      ulong ticket = HistoryDealGetTicket(i);
      if(ticket == 0) continue;
      long entry = HistoryDealGetInteger(ticket, DEAL_ENTRY);
      long type  = HistoryDealGetInteger(ticket, DEAL_TYPE);
      if(entry != DEAL_ENTRY_OUT && entry != DEAL_ENTRY_INOUT && entry != DEAL_ENTRY_OUT_BY) continue;
      if(type != DEAL_TYPE_BUY && type != DEAL_TYPE_SELL) continue;
      datetime tm = (datetime)HistoryDealGetInteger(ticket, DEAL_TIME);
      if(tm <= lastSent) continue;
      if(tm > maxT) maxT = tm;

      // a closing SELL deal closes a BUY position and vice versa: send the POSITION direction
      string dir = (type == DEAL_TYPE_SELL) ? "buy" : "sell";
      string item = "{\"ticket\":" + (string)ticket +
                    ",\"symbol\":\"" + Esc(HistoryDealGetString(ticket, DEAL_SYMBOL)) + "\"" +
                    ",\"type\":\"" + dir + "\"" +
                    ",\"volume\":" + DoubleToString(HistoryDealGetDouble(ticket, DEAL_VOLUME), 2) +
                    ",\"profit\":" + DoubleToString(HistoryDealGetDouble(ticket, DEAL_PROFIT), 2) +
                    ",\"commission\":" + DoubleToString(HistoryDealGetDouble(ticket, DEAL_COMMISSION), 2) +
                    ",\"swap\":" + DoubleToString(HistoryDealGetDouble(ticket, DEAL_SWAP), 2) +
                    ",\"time\":\"" + TimeToString(tm, TIME_DATE | TIME_SECONDS) + "\"}";
      if(n > 0) deals += ",";
      deals += item;
      n++;
     }
   if(n == 0) return;

   string json = "{\"token\":\"" + Esc(Token) + "\",\"platform\":\"MT5\",\"account\":" +
                 (string)AccountInfoInteger(ACCOUNT_LOGIN) + ",\"deals\":[" + deals + "]}";
   char post[]; char result[]; string resHeaders;
   int len = StringToCharArray(json, post, 0, StringLen(json), CP_UTF8);
   ArrayResize(post, len);
   int code = WebRequest("POST", PhoneUrl, "Content-Type: application/json\r\n", 5000, post, result, resHeaders);
   if(code == 200)
     {
      lastSent = maxT;
      GlobalVariableSet(gvKey, (double)(long)lastSent);
      Print("TradixBridge: sent ", n, " deal(s).");
     }
   else
      Print("TradixBridge: send failed, HTTP ", code, " (error ", GetLastError(),
            "). Is TRADIX bridge running and the URL allowed in Options > Expert Advisors?");
  }
