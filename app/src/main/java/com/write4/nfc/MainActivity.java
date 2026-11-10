package com.write4.nfc;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.nfc.*;
import android.nfc.tech.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {
 private final int BG=0xff0b1220, PANEL=0xff162237, INK=0xffedf4ff, MUTED=0xffa1b2ca, LIME=0xffb5ff68;
 private LinearLayout root, form; private TextView status,result,bytes; private EditText name,phone,email,company,web;
 private NfcAdapter adapter; private volatile NdefMessage pending; private final AtomicBoolean busy=new AtomicBoolean();
 private final ExecutorService io=Executors.newSingleThreadExecutor(); private boolean contact=true; private String lastUrl;
 private android.content.SharedPreferences prefs;
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);prefs=getSharedPreferences("draft",0);adapter=NfcAdapter.getDefaultAdapter(this);build();}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(6),0,dp(6));return v;}
 private void title(String s){TextView t=text(s,27,INK);t.setTypeface(null,Typeface.BOLD);root.addView(t);}
 private Button button(String label,boolean primary,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(primary?BG:INK);b.setTextSize(15);b.setBackground(shape(primary?LIME:PANEL,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(8),0,dp(4));b.setLayoutParams(lp);b.setOnClickListener(v->action.run());return b;}
 private EditText field(String hint,String key,int type){form.addView(text(hint,12,MUTED));EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(INK);e.setTextSize(16);e.setInputType(type);e.setPadding(dp(15),dp(10),dp(15),dp(10));e.setBackground(shape(PANEL,12));e.setText(prefs.getString(key,""));form.addView(e,new LinearLayout.LayoutParams(-1,dp(50)));return e;}
 private void build(){ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(24),dp(28),dp(24),dp(24));scroll.addView(root);setContentView(scroll);
 root.setOnApplyWindowInsetsListener((v,insets)->{int top=insets.getSystemWindowInsetTop(), bottom=insets.getSystemWindowInsetBottom();v.setPadding(dp(24),dp(20)+top,dp(24),dp(24)+bottom);return insets;});
 root.addView(text("WRITE4    /    NFC STUDIO",12,LIME));title("Un toque.\nToda tu información.");root.addView(text("Crea una tarjeta que conecte contigo.",15,MUTED));
 status=text("",14,LIME);root.addView(status);
 LinearLayout tabs=new LinearLayout(this);Button c=button("◉  Contacto",contact,()->switchMode(true));Button w=button("↗  Sitio web",!contact,()->switchMode(false));tabs.addView(c,new LinearLayout.LayoutParams(0,dp(52),1));tabs.addView(w,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(tabs);
 form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);root.addView(form);
 if(contact){name=field("Nombre completo *","name",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);company=field("Empresa / organización","company",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);phone=field("Teléfono","phone",InputType.TYPE_CLASS_PHONE);email=field("Correo electrónico","email",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);}
 web=field(contact?"Sitio web (opcional)":"Dirección de tu sitio *","web",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
 bytes=text("NDEF estándar · contacto vCard o enlace HTTPS",12,MUTED);root.addView(bytes);
 root.addView(button("Vista previa",false,()->preview()));root.addView(button("Grabar tarjeta NFC  →",true,()->arm()));
 root.addView(button("Leer una tarjeta",false,()->{pending=null;status.setText("Modo lectura · acerca una etiqueta NFC");}));
 root.addView(button("Cancelar grabación",false,()->{pending=null;updateStatus();}));
 root.addView(text("LECTURA DE TARJETA",12,LIME));result=text("Acerca una etiqueta para ver su contenido, capacidad y estado.",14,MUTED);result.setTextIsSelectable(true);root.addView(result);
 root.addView(button("Abrir enlace leído",false,()->{if(lastUrl!=null)new AlertDialog.Builder(this).setTitle("Abrir sitio").setMessage(lastUrl).setPositiveButton("Abrir",(d,id)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(lastUrl)));}catch(ActivityNotFoundException e){toast("No hay navegador disponible");}}).setNegativeButton("Cancelar",null).show();else toast("Primero lee una etiqueta con enlace web");}));
 root.addView(button("Ajustes NFC",false,()->startActivity(new Intent(Settings.ACTION_NFC_SETTINGS))));root.addView(text("Mantén la tarjeta quieta durante la grabación. Su contenido anterior será reemplazado. No se bloquea la tarjeta.",12,MUTED));updateStatus();}
 private void save(){android.content.SharedPreferences.Editor e=prefs.edit();if(contact)e.putString("name",name.getText().toString()).putString("company",company.getText().toString()).putString("phone",phone.getText().toString()).putString("email",email.getText().toString());if(web!=null)e.putString("web",web.getText().toString());e.apply();}
 private void switchMode(boolean c){save();pending=null;contact=c;build();}
 private void updateStatus(){status.setText(adapter==null?"Este equipo no tiene NFC":!adapter.isEnabled()?"NFC apagado · actívalo en Ajustes":"● NFC listo · modo lectura");}
 private String value(EditText e){return e.getText().toString().trim();}
 private String url(String s){if(s.isEmpty())return "";if(!s.contains("://"))s="https://"+s;Uri u=Uri.parse(s);if(!("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme()))||u.getHost()==null||u.getHost().isEmpty()||s.matches(".*\\s.*"))throw new IllegalArgumentException("Escribe un enlace web válido (https://ejemplo.com)");return s;}
 private String esc(String s){return s.replace("\\","\\\\").replace("\r\n","\n").replace("\r","\n").replace("\n","\\n").replace(";","\\;").replace(",","\\,");}
 private String fold(String line){StringBuilder out=new StringBuilder();int count=0;for(int i=0;i<line.length();){int cp=line.codePointAt(i);String ch=new String(Character.toChars(cp));int n=ch.getBytes(StandardCharsets.UTF_8).length;if(count+n>75){out.append("\r\n ");count=1;}out.append(ch);count+=n;i+=Character.charCount(cp);}return out+"\r\n";}
 private NdefMessage message(){save();String link=url(value(web));if(!contact){if(link.isEmpty())throw new IllegalArgumentException("Agrega el sitio que quieres grabar");return new NdefMessage(NdefRecord.createUri(link));}
 if(value(name).isEmpty())throw new IllegalArgumentException("Agrega el nombre del contacto");if(!value(email).isEmpty()&&!android.util.Patterns.EMAIL_ADDRESS.matcher(value(email)).matches())throw new IllegalArgumentException("Revisa el correo electrónico");
 String card=fold("BEGIN:VCARD")+fold("VERSION:3.0")+fold("N:;"+esc(value(name))+";;;")+fold("FN:"+esc(value(name)));
 if(!value(company).isEmpty())card+=fold("ORG:"+esc(value(company)));if(!value(phone).isEmpty())card+=fold("TEL;TYPE=CELL:"+esc(value(phone)));if(!value(email).isEmpty())card+=fold("EMAIL;TYPE=INTERNET:"+esc(value(email)));if(!link.isEmpty())card+=fold("URL:"+esc(link));card+=fold("END:VCARD");return new NdefMessage(NdefRecord.createMime("text/vcard",card.getBytes(StandardCharsets.UTF_8)));}
 private void preview(){try{NdefMessage m=message();bytes.setText(m.toByteArray().length+" bytes · comprueba la capacidad de tu etiqueta");new AlertDialog.Builder(this).setTitle(contact?"Tu tarjeta de contacto":"Tu enlace web").setMessage(describe(m)).setPositiveButton("Listo",null).show();}catch(Exception e){toast(e.getMessage());}}
 private void arm(){try{if(adapter==null||!adapter.isEnabled()){toast("Activa NFC en un teléfono compatible");return;}NdefMessage m=message();new AlertDialog.Builder(this).setTitle("Grabar etiqueta").setMessage(describe(m)+"\n\n"+m.toByteArray().length+" bytes. Se reemplazará el contenido de la siguiente etiqueta que acerques.").setPositiveButton("Preparar grabación",(d,w)->{pending=m;status.setText("Lista para grabar · acerca UNA tarjeta");}).setNegativeButton("Cancelar",null).show();}catch(Exception e){toast(e.getMessage());}}
 @Override protected void onResume(){super.onResume();updateStatus();if(adapter!=null&&adapter.isEnabled())adapter.enableReaderMode(this,this,NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_NFC_F|NfcAdapter.FLAG_READER_NFC_V|NfcAdapter.FLAG_READER_NFC_BARCODE,null);}
 @Override protected void onPause(){pending=null;save();if(adapter!=null)adapter.disableReaderMode(this);super.onPause();}
 @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
 @Override public void onTagDiscovered(Tag tag){if(!busy.compareAndSet(false,true))return;NdefMessage write=pending;pending=null;io.execute(()->{try{if(write!=null)write(tag,write);else read(tag);}catch(Exception e){show("No se completó: "+(e instanceof TagLostException?"Se alejó la tarjeta. Vuelve a acercarla.":e.getMessage()));}finally{busy.set(false);}});}
 private void write(Tag tag,NdefMessage m)throws Exception{Ndef n=Ndef.get(tag);if(n!=null){try{n.connect();if(!n.isWritable())throw new Exception("La etiqueta es de solo lectura");if(m.toByteArray().length>n.getMaxSize())throw new Exception("Capacidad insuficiente: necesitas "+m.toByteArray().length+" bytes; disponibles "+n.getMaxSize());n.writeNdefMessage(m);NdefMessage actual=n.getNdefMessage();if(actual==null||!Arrays.equals(m.toByteArray(),actual.toByteArray()))throw new Exception("No fue posible verificar la grabación. Lee la etiqueta de nuevo");}finally{try{n.close();}catch(Exception ignored){}}show("✓ Tarjeta grabada y verificada\n\n"+describe(m));}else{NdefFormatable f=NdefFormatable.get(tag);if(f==null)throw new Exception("Etiqueta no compatible con NDEF");try{f.connect();f.format(m);}finally{try{f.close();}catch(Exception ignored){}}show("Grabación realizada. Retira y acerca la tarjeta para verificar su contenido.");}}
 private void read(Tag tag)throws Exception{Ndef n=Ndef.get(tag);if(n==null){show("Etiqueta detectada · sin datos NDEF legibles\nTecnologías: "+String.join(", ",tag.getTechList()));return;}try{n.connect();NdefMessage m=n.getNdefMessage();String details="Capacidad: "+n.getMaxSize()+" bytes · "+(n.isWritable()?"Regrabable":"Solo lectura")+"\n\n"+(m==null?"Etiqueta vacía":describe(m));String found=null;if(m!=null)for(NdefRecord r:m.getRecords()){Uri u=r.toUri();if(u!=null&&("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme())))found=u.toString();}String link=found;show(details);runOnUiThread(()->lastUrl=link);}finally{try{n.close();}catch(Exception ignored){}}}
 private String describe(NdefMessage m){StringBuilder b=new StringBuilder();for(NdefRecord r:m.getRecords()){Uri u=r.toUri();if(u!=null){b.append("Enlace: ").append(u).append("\n");continue;}byte[] p=r.getPayload();if(r.getTnf()==NdefRecord.TNF_WELL_KNOWN&&Arrays.equals(r.getType(),NdefRecord.RTD_TEXT)&&p.length>0){int offset=1+(p[0]&63);if(offset<=p.length)b.append(new String(p,offset,p.length-offset,(p[0]&128)==0?StandardCharsets.UTF_8:StandardCharsets.UTF_16));}else if(r.toMimeType()!=null){b.append(r.toMimeType()).append("\n").append(new String(p,StandardCharsets.UTF_8));}else b.append("Registro NDEF · ").append(p.length).append(" bytes");b.append("\n");}return b.toString();}
 private void show(String s){runOnUiThread(()->{lastUrl=null;result.setText(s);status.setText(s.startsWith("✓")?"● Grabación completa":"● NFC listo · modo lectura");});}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
