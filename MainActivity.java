package in.jec.jagenelectriciancompany;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_LOCATION = 44, REQ_FILE = 55;
    private final int NAVY = Color.rgb(16,24,39), GOLD = Color.rgb(201,168,106), BG = Color.rgb(244,246,250);
    private LinearLayout root, box;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FusedLocationProviderClient locationClient;
    private String selectedRole = "employee", verificationId;
    private EditText phoneInput, otpInput;
    private String pendingUploadType = "bill";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        splash();
    }

    private TextView text(String s, int size) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(NAVY);
        v.setPadding(12,12,12,12); return v;
    }
    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(17); return b;
    }
    private EditText input(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextSize(17); e.setPadding(16,14,16,14); return e;
    }
    private void addSpace(int h) { Space s=new Space(this); box.addView(s,new LinearLayout.LayoutParams(1,h)); }
    private void clear(String heading) { box.removeAllViews(); TextView t=text(heading,26); t.setPadding(8,18,8,18); box.addView(t); }
    private void base() {
        ScrollView scroll = new ScrollView(this); root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setPadding(20,0,20,30); scroll.addView(root); setContentView(scroll);
        TextView h=text("J⚡C · JAGEN ELECTRICIAN COMPANY\nFIREBASE · GPS · CLOUD STORAGE",22); h.setTextColor(Color.WHITE); h.setBackgroundColor(NAVY); h.setPadding(20,26,20,24); root.addView(h,new LinearLayout.LayoutParams(-1,-2));
        View line=new View(this); line.setBackgroundColor(GOLD); root.addView(line,new LinearLayout.LayoutParams(-1,5));
        box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(12,20,12,12); root.addView(box);
    }

    private void splash() {
        LinearLayout s=new LinearLayout(this); s.setGravity(Gravity.CENTER); s.setOrientation(LinearLayout.VERTICAL); s.setBackgroundColor(NAVY);
        TextView logo=text("J⚡C",64); logo.setTextColor(Color.WHITE); logo.setGravity(Gravity.CENTER);
        TextView n=text("JAGEN ELECTRICIAN COMPANY",20); n.setTextColor(Color.WHITE); n.setGravity(Gravity.CENTER);
        TextView sub=text("ELECTRICAL SERVICE · GPS · CLOUD",13); sub.setTextColor(GOLD); sub.setGravity(Gravity.CENTER);
        s.addView(logo); s.addView(n); s.addView(sub); setContentView(s);
        ScaleAnimation scale=new ScaleAnimation(.65f,1.08f,.65f,1.08f,Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f); scale.setDuration(800); scale.setFillAfter(true); logo.startAnimation(scale);
        AlphaAnimation fade=new AlphaAnimation(0f,1f); fade.setDuration(1100); n.startAnimation(fade); sub.startAnimation(fade);
        new Handler(Looper.getMainLooper()).postDelayed(() -> { if(auth.getCurrentUser()!=null) loadExistingRole(); else login(); },1600);
    }

    private void login() {
        base(); clear("Secure Login"); box.addView(text("Select your account type",15));
        Button admin=button("ADMIN LOGIN"), employee=button("EMPLOYEE LOGIN"); box.addView(admin); box.addView(employee);
        admin.setOnClickListener(v->{selectedRole="admin"; phoneStep();}); employee.setOnClickListener(v->{selectedRole="employee"; phoneStep();});
    }
    private void phoneStep() {
        clear(selectedRole.equals("admin")?"Admin Login":"Employee Login");
        box.addView(text("Firebase Phone OTP verification",14)); phoneInput=input("10-digit mobile number"); phoneInput.setInputType(InputType.TYPE_CLASS_PHONE); box.addView(phoneInput);
        Button send=button("Send OTP"); box.addView(send); send.setOnClickListener(v->sendOtp()); Button back=button("Back"); box.addView(back); back.setOnClickListener(v->login());
    }
    private String normalizedPhone() {
        String p=phoneInput.getText().toString().replaceAll("\\D",""); if(p.length()==10) return "+91"+p; if(p.startsWith("91")&&p.length()==12) return "+"+p; return p.startsWith("+")?p:"+"+p;
    }
    private void sendOtp() {
        String p=normalizedPhone(); if(!p.matches("\\+\\d{10,15}")){toast("Enter a valid mobile number");return;}
        PhoneAuthOptions options=PhoneAuthOptions.newBuilder(auth).setPhoneNumber(p).setTimeout(60L, TimeUnit.SECONDS).setActivity(this)
            .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                @Override public void onVerificationCompleted(@NonNull PhoneAuthCredential c){ signInCredential(c); }
                @Override public void onVerificationFailed(@NonNull FirebaseException e){ toast("OTP error: "+safe(e.getMessage())); }
                @Override public void onCodeSent(@NonNull String id,@NonNull PhoneAuthProvider.ForceResendingToken token){ verificationId=id; otpStep(); }
            }).build();
        PhoneAuthProvider.verifyPhoneNumber(options); toast("Sending OTP…");
    }
    private void otpStep() {
        clear("Verify OTP"); otpInput=input("6-digit OTP"); otpInput.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(otpInput);
        Button verify=button("Verify OTP"); box.addView(verify); verify.setOnClickListener(v->{String code=otpInput.getText().toString().trim(); if(verificationId==null||code.length()!=6){toast("Enter the 6-digit OTP");return;} signInCredential(PhoneAuthProvider.getCredential(verificationId,code));});
    }
    private void signInCredential(PhoneAuthCredential c){auth.signInWithCredential(c).addOnSuccessListener(x->roleGate()).addOnFailureListener(e->toast("OTP verification failed: "+safe(e.getMessage())));}
    private void loadExistingRole(){FirebaseUser u=auth.getCurrentUser(); if(u==null){login();return;} db.collection("users").document(u.getUid()).get().addOnSuccessListener(d->{String r=d.getString("role"); if(r==null){auth.signOut();login();} else dashboard(r);}).addOnFailureListener(e->{auth.signOut();login();});}
    private void roleGate() {
        FirebaseUser u=auth.getCurrentUser(); if(u==null){login();return;}
        db.collection("users").document(u.getUid()).get().addOnSuccessListener(d->{
            String existing=d.getString("role");
            if(existing!=null && !existing.equals(selectedRole)){auth.signOut(); toast("This number is registered as "+existing); login(); return;}
            if(existing!=null){dashboard(existing);return;}
            Map<String,Object> m=new HashMap<>(); m.put("phone",u.getPhoneNumber()); m.put("role",selectedRole); m.put("active",true); m.put("createdAt", FieldValue.serverTimestamp());
            db.collection("users").document(u.getUid()).set(m).addOnSuccessListener(x->dashboard(selectedRole)).addOnFailureListener(e->toast("User setup failed: "+safe(e.getMessage())));
        }).addOnFailureListener(e->toast("Role check failed: "+safe(e.getMessage())));
    }

    private void dashboard(String role) {
        base(); clear(role.equals("admin")?"ADMIN DASHBOARD":"EMPLOYEE DASHBOARD"); FirebaseUser u=auth.getCurrentUser(); box.addView(text(u!=null?String.valueOf(u.getPhoneNumber()):"",14));
        if(role.equals("admin")) adminDashboard(); else employeeDashboard();
        addSpace(14); Button logout=button("Logout"); box.addView(logout); logout.setOnClickListener(v->{auth.signOut();login();});
    }
    private void employeeDashboard(){
        Button att=button("📍 GPS Attendance"), trip=button("🚗 Trip / KM"), claim=button("⛽ Petrol / Expense Claim"), leave=button("🗓 Leave Request"), salary=button("₹ Salary Details"), profile=button("👤 My Profile");
        box.addView(att);box.addView(trip);box.addView(claim);box.addView(leave);box.addView(salary);box.addView(profile);
        att.setOnClickListener(v->attendanceScreen()); trip.setOnClickListener(v->tripScreen()); claim.setOnClickListener(v->claimScreen()); leave.setOnClickListener(v->leaveScreen()); salary.setOnClickListener(v->salaryScreen()); profile.setOnClickListener(v->profileScreen());
    }
    private void adminDashboard(){
        Button staff=button("👥 Staff"), attendance=button("📍 Attendance Records"), trips=button("🚗 Trip Records"), claims=button("⛽ Claims"), leaves=button("🗓 Leave Requests"), salary=button("₹ Salary Management");
        box.addView(staff);box.addView(attendance);box.addView(trips);box.addView(claims);box.addView(leaves);box.addView(salary);
        staff.setOnClickListener(v->adminList("users","Staff",true)); attendance.setOnClickListener(v->adminList("attendance","Attendance Records",false)); trips.setOnClickListener(v->adminList("trips","Trip Records",false)); claims.setOnClickListener(v->adminList("claims","Expense Claims",false)); leaves.setOnClickListener(v->adminList("leaveRequests","Leave Requests",false)); salary.setOnClickListener(v->salaryAdminScreen());
    }
    private Button backTo(String role){Button b=button("← Back"); box.addView(b); b.setOnClickListener(v->dashboard(role)); return b;}

    private void attendanceScreen(){clear("GPS Attendance"); box.addView(text("Current GPS coordinates and server time are saved to Firestore.",14)); Button in=button("CHECK IN"), out=button("CHECK OUT");box.addView(in);box.addView(out);in.setOnClickListener(v->saveAttendance("IN"));out.setOnClickListener(v->saveAttendance("OUT"));backTo("employee");}
    private void saveAttendance(String type){withLocation((lat,lng)->{Map<String,Object> m=baseUserMap();m.put("type",type);m.put("lat",lat);m.put("lng",lng);m.put("time",FieldValue.serverTimestamp());db.collection("attendance").add(m).addOnSuccessListener(x->toast(type+" attendance saved with GPS")).addOnFailureListener(e->toast(safe(e.getMessage())));});}

    private void tripScreen(){clear("Trip / KM Tracking"); box.addView(text("Start saves GPS. End saves GPS and straight-line distance between start/end.",14));Button start=button("START TRIP"),end=button("END TRIP");box.addView(start);box.addView(end);start.setOnClickListener(v->withLocation((a,b)->{getPreferences(0).edit().putLong("tripLat",Double.doubleToRawLongBits(a)).putLong("tripLng",Double.doubleToRawLongBits(b)).putLong("tripTime",System.currentTimeMillis()).apply();toast("Trip started");}));end.setOnClickListener(v->endTrip());backTo("employee");}
    private void endTrip(){long la=getPreferences(0).getLong("tripLat",Long.MIN_VALUE),lo=getPreferences(0).getLong("tripLng",Long.MIN_VALUE);if(la==Long.MIN_VALUE){toast("Start the trip first");return;}double slat=Double.longBitsToDouble(la),slng=Double.longBitsToDouble(lo);withLocation((elat,elng)->{float[] r=new float[1];Location.distanceBetween(slat,slng,elat,elng,r);Map<String,Object> m=baseUserMap();m.put("startLat",slat);m.put("startLng",slng);m.put("endLat",elat);m.put("endLng",elng);m.put("distanceKm",r[0]/1000.0);m.put("startedAtMillis",getPreferences(0).getLong("tripTime",0));m.put("endedAt",FieldValue.serverTimestamp());db.collection("trips").add(m).addOnSuccessListener(x->{getPreferences(0).edit().remove("tripLat").remove("tripLng").remove("tripTime").apply();toast(String.format(Locale.US,"Trip saved: %.2f km",r[0]/1000.0));});});}

    private void claimScreen(){clear("Petrol / Expense Claim");EditText amount=input("Amount ₹"),note=input("Purpose / notes");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);box.addView(amount);box.addView(note);Button submit=button("SUBMIT CLAIM"),upload=button("UPLOAD BILL / PHOTO");box.addView(submit);box.addView(upload);submit.setOnClickListener(v->{Map<String,Object> m=baseUserMap();m.put("amount",amount.getText().toString().trim());m.put("note",note.getText().toString().trim());m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());db.collection("claims").add(m).addOnSuccessListener(x->toast("Claim submitted"));});upload.setOnClickListener(v->{pendingUploadType="claim";chooseFile();});backTo("employee");}
    private void leaveScreen(){clear("Leave Request");EditText from=input("From date (DD-MM-YYYY)"),to=input("To date (DD-MM-YYYY)"),reason=input("Reason");box.addView(from);box.addView(to);box.addView(reason);Button submit=button("SUBMIT LEAVE");box.addView(submit);submit.setOnClickListener(v->{Map<String,Object> m=baseUserMap();m.put("from",from.getText().toString().trim());m.put("to",to.getText().toString().trim());m.put("reason",reason.getText().toString().trim());m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());db.collection("leaveRequests").add(m).addOnSuccessListener(x->toast("Leave request submitted"));});backTo("employee");}
    private void salaryScreen(){clear("Salary Details");String uid=auth.getUid();db.collection("salary").document(uid).get().addOnSuccessListener(d->{if(!d.exists())box.addView(text("No salary record added yet.",16));else box.addView(text("Monthly Salary: ₹"+String.valueOf(d.get("monthlySalary"))+"\nLast payment: "+String.valueOf(d.get("lastPayment"))+"\nNotes: "+String.valueOf(d.get("notes")),17));});backTo("employee");}
    private void profileScreen(){clear("My Profile");FirebaseUser u=auth.getCurrentUser();box.addView(text("Phone: "+(u==null?"":u.getPhoneNumber())+"\nUID: "+(u==null?"":u.getUid()),15));Button upload=button("UPLOAD PROFILE / DOCUMENT");box.addView(upload);upload.setOnClickListener(v->{pendingUploadType="profile";chooseFile();});backTo("employee");}

    private void adminList(String collection,String heading,boolean users){clear(heading);box.addView(text("Loading…",14));db.collection(collection).limit(100).get().addOnSuccessListener(q->{clear(heading);if(q.isEmpty())box.addView(text("No records found.",15));for(QueryDocumentSnapshot d:q){StringBuilder s=new StringBuilder();for(Map.Entry<String,Object> e:d.getData().entrySet()){if(e.getKey().toLowerCase(Locale.US).contains("lat")||e.getKey().toLowerCase(Locale.US).contains("lng")) continue;s.append(e.getKey()).append(": ").append(String.valueOf(e.getValue())).append("\n");}box.addView(text(s.toString(),14));}backTo("admin");}).addOnFailureListener(e->{toast(safe(e.getMessage()));backTo("admin");});}
    private void salaryAdminScreen(){clear("Salary Management");EditText uid=input("Employee Firebase UID"),amount=input("Monthly salary ₹"),note=input("Notes");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);box.addView(uid);box.addView(amount);box.addView(note);Button save=button("SAVE SALARY");box.addView(save);save.setOnClickListener(v->{String id=uid.getText().toString().trim();if(id.isEmpty()){toast("Enter employee UID");return;}Map<String,Object> m=new HashMap<>();m.put("monthlySalary",amount.getText().toString().trim());m.put("notes",note.getText().toString().trim());m.put("lastPayment",new SimpleDateFormat("dd-MM-yyyy",Locale.US).format(new Date()));m.put("updatedAt",FieldValue.serverTimestamp());db.collection("salary").document(id).set(m).addOnSuccessListener(x->toast("Salary record saved"));});backTo("admin");}

    private Map<String,Object> baseUserMap(){Map<String,Object> m=new HashMap<>();FirebaseUser u=auth.getCurrentUser();m.put("uid",u==null?"":u.getUid());m.put("phone",u==null?"":u.getPhoneNumber());return m;}
    private interface LocationCallback{void ok(double lat,double lng);}
    private void withLocation(LocationCallback cb){if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOCATION);toast("Allow location permission, then tap again");return;}locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,null).addOnSuccessListener(l->{if(l==null)toast("GPS location unavailable. Turn Location ON.");else cb.ok(l.getLatitude(),l.getLongitude());}).addOnFailureListener(e->toast("GPS error: "+safe(e.getMessage())));}
    private void chooseFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_FILE);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQ_FILE&&resultCode==Activity.RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();String uid=auth.getUid();if(uid==null){toast("Login required");return;}String path="uploads/"+uid+"/"+pendingUploadType+"/"+System.currentTimeMillis();storage.getReference().child(path).putFile(uri).addOnSuccessListener(t->t.getStorage().getDownloadUrl().addOnSuccessListener(url->{Map<String,Object> m=baseUserMap();m.put("type",pendingUploadType);m.put("path",path);m.put("url",url.toString());m.put("createdAt",FieldValue.serverTimestamp());db.collection("uploads").add(m);toast("File uploaded to Firebase Storage");})).addOnFailureListener(e->toast("Upload failed: "+safe(e.getMessage())));}}
    private String safe(String s){return s==null?"Unknown error":s;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
