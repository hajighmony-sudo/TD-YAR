package com.tradenex.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import kotlin.concurrent.thread

/**
 * "مشترک": serves a read-only, TV-friendly dashboard of the journal over the local Wi-Fi.
 * Open http://<phone-ip>:8790 in any browser on the TV (Android TV / Google TV browsers, or cast Chrome) and enter the PIN.
 * Nothing leaves the local network; the server only runs while it is started from the app.
 */
object TvShare {
    var running by mutableStateOf(false); private set
    var requests by mutableIntStateOf(0); private set
    var port = 8790
    var pin by mutableStateOf(newPin())
    @Volatile private var snapshot: String = "{}"
    @Volatile private var activePin: String = ""
    @Volatile private var server: ServerSocket? = null
    private val main = Handler(Looper.getMainLooper())

    fun newPin(): String = (SecureRandom().nextInt(900000) + 100000).toString()

    /** Call on the main thread whenever journal data changes. */
    fun publish(accounts: List<TradingAccount>, trades: List<Trade>, selectedId: String) {
        val o = JSONObject()
        o.put("generated", System.currentTimeMillis())
        o.put("selected", selectedId)
        o.put("accounts", JSONArray().apply { accounts.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) } })
        o.put("trades", JSONArray().apply {
            trades.sortedByDescending { it.closedAtMillis.takeIf { m -> m > 0 } ?: (it.epochDay * 86_400_000L) }.take(1500).forEach {
                put(JSONObject().put("id", it.id).put("symbol", it.symbol).put("buy", it.buy).put("lot", it.lot).put("pnl", it.pnl)
                    .put("strategy", it.strategy).put("emotion", it.emotion).put("day", it.epochDay).put("account", it.accountId)
                    .put("tags", JSONArray(it.tags)))
            }
        })
        snapshot = o.toString()
    }

    fun start() {
        if (running) return
        activePin = pin
        thread(isDaemon = true, name = "tradix-tv-share") {
            val ss = try { ServerSocket(port) } catch (e: Exception) { return@thread }
            server = ss
            main.post { running = true }
            while (!ss.isClosed) {
                val s = try { ss.accept() } catch (e: Exception) { break }
                thread(isDaemon = true) { runCatching { handle(s) }; runCatching { s.close() } }
            }
            main.post { running = false }
        }
    }

    fun stop() {
        runCatching { server?.close() }
        server = null
        running = false
    }

    private fun readLine(i: InputStream): String? {
        val b = ByteArrayOutputStream()
        while (true) {
            val c = i.read()
            if (c < 0) return if (b.size() == 0) null else b.toString("UTF-8")
            if (c == '\n'.code) return b.toString("UTF-8").trimEnd('\r')
            b.write(c)
            if (b.size() > 8192) return null
        }
    }

    private fun reply(s: Socket, code: Int, type: String, text: String) {
        val body = text.toByteArray()
        val head = "HTTP/1.1 $code ${if (code == 200) "OK" else "ERR"}\r\nContent-Type: $type; charset=utf-8\r\nCache-Control: no-store\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
        s.getOutputStream().apply { write(head.toByteArray()); write(body); flush() }
    }

    private fun handle(s: Socket) {
        s.soTimeout = 8000
        val input = s.getInputStream()
        val request = readLine(input) ?: return
        while (true) { val l = readLine(input) ?: break; if (l.isEmpty()) break }
        val target = request.split(" ").getOrNull(1) ?: "/"
        val path = target.substringBefore('?')
        val q = target.substringAfter('?', "").split('&').mapNotNull { it.split('=').takeIf { p -> p.size == 2 }?.let { p -> p[0] to p[1] } }.toMap()
        main.post { requests += 1 }
        when (path) {
            "/" -> reply(s, 200, "text/html", PAGE)
            "/data.json" ->
                if (q["pin"] == activePin && activePin.isNotBlank()) reply(s, 200, "application/json", snapshot)
                else reply(s, 401, "application/json", "{\"error\":\"pin\"}")
            else -> reply(s, 404, "text/plain", "not found")
        }
    }

    // No dollar signs inside: this is a plain (non-template) Kotlin string.
    private const val PAGE = """<!doctype html><html lang="fa" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>TRADIX TV</title><style>
:root{--g:#d4af37;--bg:#0b0b0d;--c:#16161a;--t:#f2efe6;--m:#9a968a;--up:#3ddc97;--dn:#ff5d5d}
*{box-sizing:border-box}body{margin:0;background:radial-gradient(1200px 600px at 80% -10%,#2a2410,#0b0b0d 60%);color:var(--t);font-family:system-ui,Tahoma,sans-serif;font-size:2.2vmin;padding:3vmin}
h1{margin:0 0 2vmin;color:var(--g);letter-spacing:.3vmin;font-size:4vmin}.row{display:flex;gap:2vmin;flex-wrap:wrap;margin-bottom:2vmin}
.card{background:var(--c);border:1px solid #3a331a;border-radius:2vmin;padding:2.4vmin;flex:1;min-width:22vmin}
.k{color:var(--m);font-size:2vmin}.v{font-size:5vmin;font-weight:800;margin-top:.6vmin}.up{color:var(--up)}.dn{color:var(--dn)}
table{width:100%;border-collapse:collapse}td,th{padding:1vmin 1.2vmin;text-align:right;border-bottom:1px solid #26262c}th{color:var(--m);font-weight:600}
canvas{width:100%;height:30vmin;display:block}#pin{display:flex;flex-direction:column;align-items:center;gap:2vmin;margin-top:20vmin}
input{font-size:5vmin;text-align:center;padding:1.5vmin;border-radius:1.5vmin;border:1px solid var(--g);background:#111;color:var(--t);width:40vmin}
button{font-size:3vmin;padding:1.5vmin 4vmin;border-radius:1.5vmin;border:0;background:var(--g);color:#111;font-weight:800}
select{font-size:2.2vmin;padding:.8vmin;background:#111;color:var(--t);border:1px solid #3a331a;border-radius:1vmin}
</style></head><body>
<div id="pin"><h1>TRADIX TV</h1><div>کد ۶ رقمی نمایش‌داده‌شده در برنامه را وارد کن</div><input id="p" inputmode="numeric" maxlength="6"><button onclick="go()">ورود</button><div id="e" class="dn"></div></div>
<div id="app" style="display:none"><h1>TRADIX · مشترک <select id="acc" onchange="draw()"></select></h1>
<div class="row" id="stats"></div><div class="card"><div class="k">منحنی سرمایه (سود تجمعی)</div><canvas id="cv" width="1600" height="400"></canvas></div>
<div class="card" style="margin-top:2vmin"><div class="k">آخرین معامله‌ها</div><table id="tb"></table></div></div>
<script>
var PIN="",D=null;
function go(){PIN=document.getElementById('p').value;load(true)}
function load(first){fetch('/data.json?pin='+PIN).then(function(r){if(!r.ok)throw 0;return r.json()}).then(function(j){D=j;
 if(first){document.getElementById('pin').style.display='none';document.getElementById('app').style.display='block';
 var s=document.getElementById('acc');s.innerHTML='<option value="">همه حساب‌ها</option>'+j.accounts.map(function(a){return '<option value="'+a.id+'"'+(a.id==j.selected?' selected':'')+'>'+a.name+'</option>'}).join('');setInterval(function(){load(false)},5000)}
 draw()}).catch(function(){if(first)document.getElementById('e').textContent='کد اشتباه یا برنامه در دسترس نیست'})}
function f(v){return (Math.round(v*100)/100).toLocaleString('en-US')}
function draw(){var a=document.getElementById('acc').value,T=D.trades.filter(function(t){return !a||t.account==a}).slice().sort(function(x,y){return x.day-y.day||x.id-y.id});
 var net=0,w=0,gp=0,gl=0,i;for(i=0;i<T.length;i++){var p=T[i].pnl;net+=p;if(p>0){w++;gp+=p}else if(p<0){gl-=p}}
 var pf=gl==0?(gp>0?'∞':'0'):f(gp/gl),wr=T.length?Math.round(w*100/T.length):0;
 function c(k,v,cl){return '<div class="card"><div class="k">'+k+'</div><div class="v '+(cl||'')+'">'+v+'</div></div>'}
 document.getElementById('stats').innerHTML=c('سود خالص',f(net),net>=0?'up':'dn')+c('تعداد معامله',T.length)+c('نرخ برد',wr+'٪')+c('Profit Factor',pf);
 var cv=document.getElementById('cv'),x=cv.getContext('2d');x.clearRect(0,0,cv.width,cv.height);
 if(T.length>1){var e=[0],m=0;for(i=0;i<T.length;i++){m+=T[i].pnl;e.push(m)}var lo=Math.min.apply(null,e),hi=Math.max.apply(null,e);if(hi==lo)hi=lo+1;
  x.strokeStyle='#d4af37';x.lineWidth=4;x.beginPath();for(i=0;i<e.length;i++){var px=i/(e.length-1)*cv.width,py=cv.height-8-(e[i]-lo)/(hi-lo)*(cv.height-16);if(i==0)x.moveTo(px,py);else x.lineTo(px,py)}x.stroke()}
 var L=T.slice(-12).reverse();document.getElementById('tb').innerHTML='<tr><th>نماد</th><th>جهت</th><th>لات</th><th>سود/زیان</th><th>استراتژی</th></tr>'+L.map(function(t){return '<tr><td>'+t.symbol+'</td><td>'+(t.buy?'Buy':'Sell')+'</td><td>'+t.lot+'</td><td class="'+(t.pnl>=0?'up':'dn')+'">'+f(t.pnl)+'</td><td>'+(t.strategy||'')+'</td></tr>'}).join('')}
</script></body></html>"""
}
