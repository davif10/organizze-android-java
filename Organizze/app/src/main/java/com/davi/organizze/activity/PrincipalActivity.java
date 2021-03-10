package com.davi.organizze.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.TextView;
import android.widget.Toast;

import com.davi.organizze.R;
import com.davi.organizze.adapter.AdapterMovimentacao;
import com.davi.organizze.config.ConfiguracaoFirebase;
import com.davi.organizze.helper.Base64Custom;
import com.davi.organizze.model.Movimentacao;
import com.davi.organizze.model.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PrincipalActivity extends AppCompatActivity {
    private MaterialCalendarView calendarView;
    private TextView textoSaudacao,textoSaldo;
    private Double despesaTotal =0.0;
    private Double receitaTotal =0.0;
    private Double resumoUsuario =0.0;
    private FirebaseAuth autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
    private DatabaseReference firebaseref = ConfiguracaoFirebase.getFirebaseDatabase();
    private DatabaseReference usuarioRef;
    private ValueEventListener valueEventListenerUsuario;
    private ValueEventListener valueEventListenerMovimentacoes;

    private RecyclerView recyclerView;
    private AdapterMovimentacao adapterMovimentacao;
    private List<Movimentacao> movimentacoes = new ArrayList<>();
    private Movimentacao movimentacao;
    private DatabaseReference movimentacaoRef;
    private String mesAnoSelecionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Organizze");
        setSupportActionBar(toolbar);
        calendarView = findViewById(R.id.calendarView);
        textoSaldo = findViewById(R.id.textSaldo);
        textoSaudacao = findViewById(R.id.textSaudacao);
        recyclerView = findViewById(R.id.recyclerMovimentos);
        configuraCalendarView();
        swipe();

        //Configurar adapter
        adapterMovimentacao = new AdapterMovimentacao(movimentacoes,this);
        //Configurar RecyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapterMovimentacao);


            }

            public void swipe(){
                ItemTouchHelper.Callback itemTouch = new ItemTouchHelper.Callback() {
                    @Override
                    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                        int dragFlags = ItemTouchHelper.ACTION_STATE_IDLE;
                        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                        return makeMovementFlags(dragFlags,swipeFlags);
                    }

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                        excluirMovimentacao(viewHolder);
                    }
                };

                new ItemTouchHelper(itemTouch).attachToRecyclerView(recyclerView);
            }

            public void excluirMovimentacao(final RecyclerView.ViewHolder viewHolder){
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                //Configura Alert Dialog
                alertDialog.setTitle("Excluir Movimentação da Conta");
                alertDialog.setMessage("Você tem certeza que deseja realmente excluir essa movimentação da sua conta?");
                alertDialog.setCancelable(false);

                alertDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int position = viewHolder.getAdapterPosition();
                        movimentacao = movimentacoes.get(position);

                        String emailUsuario = autenticacao.getCurrentUser().getEmail();
                        String idUsuario = Base64Custom.codificarBase64(emailUsuario);
                        movimentacaoRef = firebaseref.child("movimentacao")
                                .child(idUsuario)
                                .child(mesAnoSelecionado);

                        movimentacaoRef.child(movimentacao.getKey()).removeValue();
                        adapterMovimentacao.notifyItemRemoved(position);
                        atualizarSaldo();

                    }
                });

                alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(PrincipalActivity.this,"Cancelado",Toast.LENGTH_SHORT).show();
                        adapterMovimentacao.notifyDataSetChanged();
                    }
                });

                AlertDialog alert = alertDialog.create();
                alert.show();
            }

            public void atualizarSaldo(){
                String emailUsuario = autenticacao.getCurrentUser().getEmail();
                String idUsuario = Base64Custom.codificarBase64(emailUsuario);
                usuarioRef = firebaseref.child("usuarios").child(idUsuario);

                if(movimentacao.getTipo().equals("r")){
                    receitaTotal = receitaTotal - movimentacao.getValor();
                    usuarioRef.child("receitaTotal").setValue(receitaTotal);
                }

                if(movimentacao.getTipo().equals("d")){
                    despesaTotal = despesaTotal-movimentacao.getValor();
                    usuarioRef.child("despesaTotal").setValue(despesaTotal);
                }
            }

          public void recuperarMovimentacoes(){
              String emailUsuario = autenticacao.getCurrentUser().getEmail();
              String idUsuario = Base64Custom.codificarBase64(emailUsuario);
            movimentacaoRef = firebaseref.child("movimentacao")
                                         .child(idUsuario)
                                         .child(mesAnoSelecionado);


            valueEventListenerMovimentacoes = movimentacaoRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    movimentacoes.clear();
                    for(DataSnapshot dados:dataSnapshot.getChildren()){
                        Movimentacao movimentacao = dados.getValue(Movimentacao.class);
                        movimentacao.setKey(dados.getKey());
                        movimentacoes.add(movimentacao);

                    }

                    adapterMovimentacao.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

          }

        public void recuperarResumo(){
          String emailUsuario = autenticacao.getCurrentUser().getEmail();
          String idUsuario = Base64Custom.codificarBase64(emailUsuario);
          usuarioRef = firebaseref.child("usuarios").child(idUsuario);
          System.out.println("Evento foi adicionado!");

          valueEventListenerUsuario = usuarioRef.addValueEventListener(new ValueEventListener() {
              @Override
              public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                  Usuario usuario = dataSnapshot.getValue(Usuario.class);
                  despesaTotal = usuario.getDespesaTotal();
                  receitaTotal = usuario.getReceitaTotal();
                  resumoUsuario = receitaTotal-despesaTotal;
                  DecimalFormat decimalFormat = new DecimalFormat("0.##");
                  String resultadoFormatado = decimalFormat.format(resumoUsuario);

                  textoSaldo.setText("R$ "+resultadoFormatado);
                  textoSaudacao.setText("Olá, " + usuario.getNome());

              }

              @Override
              public void onCancelled(@NonNull DatabaseError databaseError) {

              }
          });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menuSair:
                autenticacao.signOut();
                startActivity(new Intent(this,MainActivity.class));
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void adicionarReceita(View view){
        startActivity(new Intent(this,ReceitasActivity.class));
    }

    public void adicionarDespesa(View view){
        startActivity(new Intent(this, Despesas2Activity.class));
    }

    public void configuraCalendarView(){
        CharSequence meses [] = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho","Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
        CharSequence semana[] = {"Seg","Ter","Qua","Qui","Sex","Sab","Dom"};
        calendarView.setTitleMonths(meses);
        calendarView.setWeekDayLabels(semana);
        CalendarDay dataAtual = calendarView.getCurrentDate();
        String mesSelecionado =String.format("%02d",dataAtual.getMonth());
        mesAnoSelecionado = String.valueOf( mesSelecionado+ "" + dataAtual.getYear());
        calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                String mesSelecionado =String.format("%02d",date.getMonth());
                mesAnoSelecionado = String.valueOf(mesSelecionado + "" + date.getYear());
                movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);
                recuperarMovimentacoes();

            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        usuarioRef.removeEventListener(valueEventListenerUsuario);
        movimentacaoRef.removeEventListener(valueEventListenerMovimentacoes);

    }

    @Override
    protected void onStart() {
        super.onStart();
        recuperarResumo();
        recuperarMovimentacoes();
    }
}
