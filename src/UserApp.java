import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.in;

public class UserApp {
    private sys model;
    private Scanner scin;

    public static void main(String[] args) {
        UserApp app = new UserApp();
        app.run();
    }

    //calcula outra vez o preço de um artigo
    private void update_artigo(Artigo entry2, Utilizador entry){
        if(entry2.getClass().equals(Malas.class)){
            Malas mala = getMalaFromArtigo(entry2);
            mala.calcPreco(this.getModel().now());

            this.getModel().getUser().remove(entry2.getID());
            if(mala.getStock() == mala.getSold()) mala.privar();
            entry.getArtigos().put(mala.getID(), mala);
        } else if(entry2.getClass().equals(Sapatilhas.class)){
            Sapatilhas shoe = getShoeFromArtigo(entry2);
            shoe.calcPreco(this.getModel().now());

            this.getModel().getUser().remove(entry2.getID());
            if(shoe.getStock() == shoe.getSold()) shoe.privar();
            entry.getArtigos().put(shoe.getID(), shoe);
        } else if(entry2.getClass().equals(TShirt.class)){
            TShirt tshirt = getTshirtFromArtigo(entry2);
            tshirt.calcPreco();

            this.getModel().getUser().remove(entry2.getID());
            if(tshirt.getStock() == tshirt.getSold()) tshirt.privar();
            entry.getArtigos().put(tshirt.getID(), tshirt);
        }
    }

    private void updates(){
        //update nos produtos vendidos
        for (Map.Entry<String, Utilizador> entry1 : this.getModel().getUser().entrySet()) {
            Utilizador logged = entry1.getValue();
            for (Map.Entry<String, Artigo> entry : logged.getArtigos().entrySet()) {
                if(entry.getValue().getSold() > 0){
                    String save = "saves/sales.txt";
                    File file = new File(save);
                    int mode;
                    if(file.exists()){
                        mode = 0;
                    } else{
                        mode = 1;
                    }
                    try{ //data, email, id do artigo
                        StringBuilder fline = new StringBuilder();
                        Map<String, Integer> already_sold = new HashMap<>();
                        if(mode == 0){
                            BufferedReader in = new BufferedReader(new FileReader(save));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while((line = in.readLine()) != null) {
                                sb.append(line);
                                sb.append("\n");
                            }
                            in.close();
                            String read = sb.toString();
                            String[] lines = read.split("\n");
                            String[] sub;
                            for (String c : lines) {
                                sub = c.split(" ");
                                if(c.length() > 5){
                                    if(!(sub[0].equals(this.getModel().now().toString()) &&
                                            sub[1].equals(logged.getEmail())
                                            && sub[2].equals(entry.getValue().getID())
                                    )){
                                        fline.append(c);
                                        fline.append("\n");
                                        if(!(already_sold.containsKey(sub[2]))){
                                            already_sold.put(sub[2], Integer.parseInt(sub[3]));
                                        } else{
                                            already_sold.put(sub[2], already_sold.get(sub[2]) + Integer.parseInt(sub[3]));
                                        }

                                    }
                                }
                            }
                        }
                        if(already_sold.size() > 0){
                            if(entry.getValue().getSold() - already_sold.get(entry.getValue().getID()) > 0){
                                this.getModel().writeTxt(save, fline + this.getModel().now().toString()
                                        + " " + logged.getEmail() + " " + entry.getValue().getID() + " " + (entry.getValue().getSold() - already_sold.get(entry.getValue().getID())) + "\n");
                            }
                        } else{
                            this.getModel().writeTxt(save, fline + this.getModel().now().toString()
                                    + " " + logged.getEmail() + " " + entry.getValue().getID() + " " + entry.getValue().getSold() + "\n");
                        }
                    } catch (IOException e){
                        System.err.println("Erro a registar ficheiro: " + e);
                    }
                }
            }
        }

        //update dos calculos de cada artigo
        for (Map.Entry<String, Utilizador> entry : this.getModel().getUser().entrySet()) {
            for (Map.Entry<String, Artigo> entry2 : entry.getValue().getArtigos().entrySet()) {
                update_artigo(entry2.getValue(), entry.getValue());
            }
        }

        //update das encomendas
        for (Map.Entry<String, Utilizador> entry : this.getModel().getUser().entrySet()) {
            for (Map.Entry<String, Encomendas> entry2 : entry.getValue().getEncomendas().entrySet()) {
                entry2.getValue().setEstado(this.getModel().now());
            }
        }
    }

    // menu da viagem no tempo
    private void viagem_tempo(){
        NewMenu viagem = new NewMenu(new String[]{
                "Futuro", "Presente", "Passado" , "Ver dia"
        });

        viagem.setHandler(1, this::futuro);
        viagem.setHandler(2, this::presente);
        viagem.setHandler(3, this::past);
        viagem.setHandler(4, this::day);

        viagem.setPreCondition(2, ()-> !(LocalDate.now().isEqual(this.getModel().now())));

        viagem.setTitle("Viagem no Tempo");

        viagem.run();
    }

    // retorna de um artigo para mala
    private Malas getMalaFromArtigo(Artigo entry2){
        Malas mala = new Malas(entry2);
        Malas mala2 = mala.fromString(entry2.toString());
        mala.setDataPremium(mala2.getDataPremium());
        mala.setMaterial(mala2.getMaterial());
        mala.setValorizacao(mala2.getValorizacao());
        mala.setTamanho(mala2.getTamanho());

        return mala;
    }

    // retorna de um artigo para sapatilha
    private Sapatilhas getShoeFromArtigo(Artigo entry2){
        Sapatilhas shoe = new Sapatilhas(entry2);
        Sapatilhas shoe2 = shoe.fromString(entry2.toString());
        shoe.setDataPremium(shoe2.getDataPremium());
        shoe.setAtacadores(shoe2.getAtacadores());
        shoe.setCor(shoe2.getCor());
        shoe.setTamanho(shoe2.getTamanho());

        return shoe;
    }

    // retorna de um artigo para Tshirt
    private TShirt getTshirtFromArtigo(Artigo entry2){
        TShirt tshirt = new TShirt(entry2);
        TShirt tshirt2 = tshirt.fromString(entry2.toString());
        tshirt.setPadrao(tshirt2.getPadrao());
        tshirt.setTamanho(tshirt2.getTamanho());

        return tshirt;
    }

    // dia atual
    private void day(){
        System.out.println("Estamos no dia: " + this.getModel().now());
    }

    // ir para o passado
    private void past(){
        System.out.println("Dias para o passado: ");
        String days = scin.nextLine(), cancel ="";
        while(Integer.parseInt(days) < 1){
            System.out.println("Dias de atraso inválido!");
            System.out.println("Cancelar a viagem no tempo [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) return;
            System.out.println("Dias para o passado: ");
            days = scin.nextLine();
        }
        this.getModel().past(Integer.parseInt(days));
        updates();
    }

    // ir para o futuro
    private void futuro(){
        System.out.println("Dias para o futuro: ");
        String days = scin.nextLine(), cancel ="";
        while(Integer.parseInt(days) < 1){
            System.out.println("Dias de avanço inválido!");
            System.out.println("Cancelar a viagem no tempo [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) return;
            System.out.println("Dias para o futuro: ");
            days = scin.nextLine();
        }
        this.getModel().future(Integer.parseInt(days));
        updates();
    }

    // ir para o presente
    private void presente(){
        this.getModel().setNow(0);
        updates();
        System.out.println("Viagem para o presente concluída!");
    }

    //construtor da app
    private UserApp(){
        model = new sys();
        scin = new Scanner(in);
        File file = new File("saves/sys.obj");
        if(file.exists()){
            try{
                this.setModel(this.getModel().load("saves/sys.obj"));
            } catch (ClassNotFoundException | IOException e) {
                System.out.println("Erro ao ler ficheiro: " + e);

                throw new RuntimeException(e);
            }
        }

    }

    // run da app
    private void run(){
        Path dir = Paths.get("saves");

        if (!Files.exists(dir)) {
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {
                System.out.println("Error creating directory: " + e.getMessage());
            }
        }

        File file = new File("saves/sales-old.txt");
        if(file.exists()){
            Path source = Paths.get("saves/sales-old.txt");
            Path target = Paths.get("saves/sales.txt");

            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("Error copying file: " + e.getMessage());
            }
        }
        updates();
        NewMenu mainMenu = new NewMenu(new String[]{
                "Log-in", "Registar", "Viagem no Tempo"
        });

        mainMenu.setHandler(1, this::login);
        mainMenu.setHandler(2, this::registar);
        mainMenu.setHandler(3, this::viagem_tempo);

        mainMenu.setPreCondition(1, ()-> model.getUser().size()  > 0 || model.getTransportadora().size() > 0);

        mainMenu.setTitle("Home");

        mainMenu.run();

        closeApp();
    }

    // guarda o sistema em "saves/sys.obj"
    private void save(){
        try{
            this.getModel().save("saves/sys.obj");
            System.out.println("Salvo!");
        } catch (IOException e){
            System.out.println("Erro a registar ficheiro: " + e);
        }
    }

    // desliga o sistem
    private void closeApp(){
        String y;

        System.out.println("Queres guardar as mudanças [y/n]?");
        y = scin.nextLine();

        if(y.equals("y")){
            save();
        }

        File file = new File("saves/sales.txt");
        if(file.exists()){
            Path source = Paths.get("saves/sales.txt");
            Path target = Paths.get("saves/sales-old.txt");

            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("Error copying file: " + e.getMessage());
            }
        }

        System.out.println("Turning off...");

    }

    // menu de funcinalidades admin
    private void admin(){
        NewMenu adminMenu = new NewMenu(new String[]{
                "Mudar % da vintage", "Ver % da vintage", "Ver coleções","Ver usuarios registados", "Eliminar usuário",
                "Ver todas as transportadoras", "Eliminar transportadora", "Ver todas as encomendas", "Ver todos artigos",
                "Ver cardapio", "Ver receita da vintage", "Vendedor que vendeu mais",
                "Transportadora que faturou mais","Guardar o sistema"
        });

        adminMenu.setHandler(1, this::change_cut_vintage);
        adminMenu.setHandler(2, this::see_vintage);
        adminMenu.setHandler(3, this::admin_colecoes);
        adminMenu.setHandler(4, this::see_users);
        adminMenu.setHandler(5, this::del_user);
        adminMenu.setHandler(6, this::see_transportadora);
        adminMenu.setHandler(7, this::del_transportadora);
        adminMenu.setHandler(8, this::see_encomendas);
        adminMenu.setHandler(9, this::see_artigos);
        adminMenu.setHandler(10, this::encomenda_cardapio);
        adminMenu.setHandler(11, this::admin_receita);
        adminMenu.setHandler(12, this::admin_seller_receita);
        adminMenu.setHandler(13, this::admin_transportadora_receita);
        adminMenu.setHandler(14, this::save);

        adminMenu.setPreCondition(3, () -> this.getModel().getColecao().size() > 0);
        adminMenu.setPreCondition(4, () -> this.getModel().getUser().size() > 0);
        adminMenu.setPreCondition(5, () -> this.getModel().getUser().size() > 0);
        adminMenu.setPreCondition(6, () -> this.getModel().getTransportadora().size() > 0);
        adminMenu.setPreCondition(7, () -> this.getModel().getTransportadora().size() > 0);
        adminMenu.setPreCondition(8, () -> this.getModel().getUser().size() > 0);
        adminMenu.setPreCondition(13, () -> this.getModel().getTransportadora().size() > 0);
        adminMenu.setPreCondition(12, () -> this.getModel().getUser().size() > 0);
        adminMenu.setTitle("Admin");
        adminMenu.run();
    }

    // mostra coleçoes
    private void admin_colecoes(){
        System.out.println(this.getModel().getColecao());
    }

    // mostra a receita da transportadora
    private void admin_transportadora_receita(){
        double maior = -1.0;
        String id = "";
        Transportadoras t = new Transportadoras(1,1,1,1);
        for(Map.Entry<String, Transportadoras> entry : this.getModel().getTransportadora().entrySet()){
            if(maior < entry.getValue().getRev()){
                t = entry.getValue();
                maior = t.getRev();
                id = entry.getKey();
            }
        }
        System.out.println("Transportadora que faturou mais: " + id + "\n" + t
                            + "\nFaturou: " + (maior - (maior * this.getModel().getVintagecut()))
        );
    }

    // menu para ver o maior vendedor
    private void admin_seller_receita(){
        NewMenu adminMenu = new NewMenu(new String[]{
                "Num intervalo de tempo", "De sempre", "Listagem num intervalo"
        });

        adminMenu.setHandler(1, ()-> this.seller_time(0));
        adminMenu.setHandler(2, this::seller_alltime);
        adminMenu.setHandler(3, ()-> this.seller_time(1));

        adminMenu.setTitle("Maior vendedor");

        adminMenu.run();
    }

    // maior vendedor (0) ou lista de maiores vendedores (1) em um determinado tempo
    private void seller_time(int i){

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date;
        while(true){
            System.out.println("Primeiro dia: (yyyy-mm-dd)");
            String d1 = scin.nextLine();
            try {
                date = LocalDate.parse(d1, formatter);
                break;
            } catch (DateTimeParseException e) {
                System.out.println("Essa data não é valida! Cancelar [y/n]? " + e.getMessage());
                if(scin.nextLine().equals("y")){
                    return;
                }
            }
        }

        LocalDate date2;

        while(true){
            System.out.println("Último dia: (yyyy-mm-dd)");
            String d2 = scin.nextLine();
            try {
                date2 = LocalDate.parse(d2, formatter);
                if(date2.isAfter(date) || date2.isEqual(date)){
                    break;
                } else{
                    System.out.println("Esta data está antes da primeira data! Cancelar [y/n]?");
                    if(scin.nextLine().equals("y")){
                        return;
                    }
                }
            } catch (DateTimeParseException e) {
                System.out.println("Essa data não é valida! Cancelar [y/n]? " + e.getMessage());
                if(scin.nextLine().equals("y")){
                    return;
                }
            }
        }

        String save = "saves/sales.txt";
        File file = new File(save);
        if(file.exists()){
            try{
                StringBuilder fline = new StringBuilder();
                BufferedReader in = new BufferedReader(new FileReader(save));
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = in.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                in.close();
                String read = sb.toString();
                String[] lines = read.split("\n");
                String[] sub;
                Map<String, Double> maxuser = new HashMap<>();
                for (String c : lines) {
                    sub = c.split(" ");
                    if((LocalDate.parse(sub[0], formatter).isAfter(date) || LocalDate.parse(sub[0], formatter).isEqual(date))
                            && (LocalDate.parse(sub[0], formatter).isBefore(date2) || LocalDate.parse(sub[0], formatter).isEqual(date2))){
                        if(maxuser.containsKey(sub[1])){
                            maxuser.put(sub[1], maxuser.get(sub[1]) + this.getModel().getUser(sub[1]).getArtigos().get(sub[2]).getPreco() * Integer.parseInt(sub[3]));
                        } else{
                            maxuser.put(sub[1], this.getModel().getUser(sub[1]).getArtigos().get(sub[2]).getPreco() * Integer.parseInt(sub[3]));
                        }
                        fline.append(c);
                    }
                }
                if(i == 0){
                    double maxValue = Double.MIN_VALUE;
                    String key = null;
                    for (Map.Entry<String, Double> entry : maxuser.entrySet()) {
                        if (entry.getValue() > maxValue) {
                            key = entry.getKey();
                            maxValue = entry.getValue();
                        }
                    }
                    System.out.println("O vendedor " + key + " foi o que mais faturou com: " +
                            (maxValue - (maxValue * this.getModel().getVintagecut())));
                } else{
                    List<Map.Entry<String, Double>> list = new ArrayList<>(maxuser.entrySet());
                    list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
                    for (Map.Entry<String, Double> entry : list) {
                        System.out.println(entry.getKey() + " faturou: " + entry.getValue());
                    }
                }
            } catch(IOException e){
                System.err.println("Erro a ler sales.txt");
            }
        } else{
            System.out.println("Não há registos de vendas.");
        }

    }

    //maior vendedor de sempre
    private void seller_alltime(){
        double maior = -1.0;
        Utilizador t = new Utilizador();
        for(Map.Entry<String, Utilizador> entry : this.getModel().getUser().entrySet()){
            if(maior < entry.getValue().getRevenue()){
                t = entry.getValue();
                maior = t.getRevenue();
            }
        }
        System.out.println("Vendedor que faturou mais: " + t.getEmail()
                + "\nFaturou: " + (maior - (maior * this.getModel().getVintagecut()))
        );
    }

    // receita da vintage
    private void admin_receita(){
        System.out.println("Receita total da Vintage: " + this.getModel().getRev());
    }

    // ver todos os artigos do sistema
    private void see_artigos(){
        System.out.println(this.getModel().getArtigos());
    }

    // ver todas as encomendas no sistema
    public void see_encomendas(){
        for(Map.Entry<String, Utilizador> entry : this.getModel().getUser().entrySet()){
            if (entry.getValue().getEncomendas().size() > 0) {
                System.out.println(entry.getValue().getEmail() +": " +  entry.getValue().getEncomendas());
            }
        }
    }

    // ver a percentagem que a vintage tira em cada compra
    public void see_vintage(){
        System.out.println(this.getModel().getVintagecut() * 100 + " %");
    }

    // alterar a percentagem que a vintage tira em cada compra
    public void change_cut_vintage(){
        String cut;
        System.out.println("Nova % da plataforma para cada venda: ");
        cut = scin.nextLine();
        this.getModel().setVintagecut((double) Integer.parseInt(cut) / 100);
    }

    // ver todas as transportadoras do sistema
    private void see_transportadora(){
        System.out.println(model.getTransportadora());
    }

    // eliminar uma transportadoras do sistema
    private void del_transportadora(){
        String id;

        System.out.println("Número da transportadora a eliminar: ");
        id = scin.nextLine();

        this.getModel().getTransportadora().remove(id);
    }

    // ver todos os utilizadores do sistema
    private void see_users(){
        System.out.println(model.getUser());
    }

    // eliminiar um utilizador do sistema
    private  void del_user(){
        String email;

        System.out.println("E-mail/ID do user a eliminar: ");
        email = scin.nextLine();

        model.DelUser(email);
    }

    // menu de login
    private void login(){
        NewMenu loginMenu = new NewMenu(new String[]{
                 "User" , "Transportadora", "Admin"
        });

        loginMenu.setHandler(1, this::login_user);
        loginMenu.setHandler(2, this::login_transportadora);
        loginMenu.setHandler(3, this::admin);

        loginMenu.setPreCondition(1, ()-> this.getModel().getUser().size()>0);
        loginMenu.setPreCondition(2, ()-> this.getModel().getTransportadora().size()>0);
        loginMenu.setTitle("Login");
        loginMenu.run();
    }

    // login como transportadora
    private void login_transportadora(){
        String id;

        System.out.println("Número da transportadora: ");
        id = scin.nextLine();

        if(this.getModel().getTransportadora().containsKey(id)){
            Transportadoras logged = model.getTransportadora().get(id);

            NewMenu userMenu = new NewMenu(new String[]{
                    "Dados", "Alterar Dados", "Ver Receita"
            });

            userMenu.setHandler(1, ()->this.details_transportadora(logged));
            userMenu.setHandler(2, ()->this.change_config_transportadora(logged));
            userMenu.setHandler(3, ()->this.rev_transportadora(logged));
            userMenu.setTitle("Transportadora Menu");
            userMenu.run();
        } else{
            System.out.println("Transportadora ainda não registada.");
        }
    }

    // receita da transportadora
    private void rev_transportadora(Transportadoras logged){
        System.out.println("Receita até ao momento: " + (logged.getRev() - (logged.getRev()*this.getModel().getVintagecut())));
    }

    // detalhes da transportadora
    private void details_transportadora(Transportadoras logged){
        if(logged.getPremium()){
            System.out.println("Imposto = " + logged.getImposto() + "\n"
                    + "Dias de atraso = " + logged.getDiasAtraso() + "\n"
                    + "Valores Base = " + logged.getValorBase().getPequeno() + ", "
                    + logged.getValorBase().getMedio() + ", "
                    + logged.getValorBase().getGrande() + ", " + "\n"
                    + "Valores de Expedição = " + logged.getPrecoExp().getPequeno() + ", "
                    + logged.getPrecoExp().getMedio() + ", "
                    + logged.getPrecoExp().getGrande() + ", " + "\n"
                    + "Valores de Expedição Premium = " + logged.getPrecoPremium().getPequeno() + ", "
                    + logged.getPrecoPremium().getMedio() + ", "
                    + logged.getPrecoPremium().getGrande() + "\n"
                    + "Formula: " + logged.getFormula() + "\n"
                    + "Formula Premium: " + logged.getFpremium()
            );
        } else{
            System.out.println("Imposto = " + logged.getImposto() + "\n"
                    + "Dias de atraso = " + logged.getDiasAtraso() + "\n"
                    + "Valores Base = " + logged.getValorBase().getPequeno() + ", "
                    + logged.getValorBase().getMedio() + ", "
                    + logged.getValorBase().getGrande() + ", " + "\n"
                    + "Valores de Expedição = " + logged.getPrecoExp().getPequeno() + ", "
                    + logged.getPrecoExp().getMedio() + ", "
                    + logged.getPrecoExp().getGrande() + "\n"
                    + "Formula: " + logged.getFormula()
            );
        }

    }

    // mudar alguma configuração da transportadora
    private void change_config_transportadora(Transportadoras logged){
        NewMenu config_trans_Menu = new NewMenu(new String[]{
                "Ativar/Desativar Premium" , "Mudar Valores Base", "Mudar Formula Expedição", "Mudar Formula Expedição Premium"
        });

        config_trans_Menu.setHandler(1, ()-> this.premium_transportadora(logged));
        config_trans_Menu.setHandler(2, ()-> this.base_transportadora(logged));
        config_trans_Menu.setHandler(3, ()-> this.formula_transportadora(logged));
        config_trans_Menu.setHandler(4, ()-> this.formula_premium_transportadora(logged));

        config_trans_Menu.setPreCondition(4, logged::getPremium);
        config_trans_Menu.setTitle("Transportadoras Config Menu");
        config_trans_Menu.run();
    }

    // mudar a formula premium da transportadora
    private void formula_premium_transportadora(Transportadoras logged){
        System.out.println("Formula nova para os preços de expedição Premium:\nkeys:\nvalor - valor base dos 3 tamanhos\nimposto - imposto");
        String formula = scin.nextLine();
        logged.setFpremium(formula);
        logged.formulaPremium(formula);

        System.out.println(logged.getPrecoPremium());
    }

    // mudar a formula da transportadora
    private void formula_transportadora(Transportadoras logged){
        System.out.println("Formula nova para os preços de expedição:\nkeys:\nvalor - valor base dos 3 tamanhos\nimposto - imposto");
        String formula = scin.nextLine();
        logged.setFormula(formula);
        logged.formula(formula);

        System.out.println(logged.getPrecoExp());
    }

    // construção da transportadora
    private void base_transportadora(Transportadoras logged){
        String base;

        System.out.println("Preço para encomendas pequenas (1 artigo): ");
        base = scin.nextLine();
        logged.getValorBase().setPequeno(Double.parseDouble(base));
        System.out.println("Preço para encomendas médias (2 a 5 artigos): ");
        base = scin.nextLine();
        logged.getValorBase().setMedio(Double.parseDouble(base));
        System.out.println("Preço para encomendas grandes (mais que 5 artigos): ");
        base = scin.nextLine();
        logged.getValorBase().setGrande(Double.parseDouble(base));

        logged.formula(logged.getFormula());

        if(logged.getPremium()){
            logged.formulaPremium(logged.getFpremium());
            System.out.println("Valores base: " + logged.getValorBase() + "\n" + "Valores expedição: " +
                    logged.getPrecoExp() +"\n"+ "Valores expedição Premium: " + logged.getPrecoPremium());
        }else{
            System.out.println("Valores base: " + logged.getValorBase() + "\n" + "Valores expedição: " +
                    logged.getPrecoExp());
        }

    }

    // ativar/desativar o premium de uma transportadora
    private void premium_transportadora(Transportadoras logged){
        if(logged.getPremium()){
            logged.desativaPremium();
            if(logged.getPrecoPremium().getPequeno() == -1.0){
                formula_transportadora(logged);
            }
        } else{
            logged.ativaPremium();
        }
    }

    // login como utilizador
    private void login_user(){
        String email;

        System.out.println("E-mail/ID: ");
        email = scin.nextLine();

        if(this.getModel().existsEmail(email) || this.getModel().getUser().containsKey(email)){
            Utilizador logged = model.getUser(email);

            NewMenu userMenu = new NewMenu(new String[]{
                    "Dados Pessoais", "Alterar Configurações", "Central de Cliente", "Centro de Vendedor"
            });

            userMenu.setHandler(1, () -> this.user_details(logged));
            userMenu.setHandler(2, () -> this.user_change_config(logged));
            userMenu.setHandler(3, () -> this.user_central_cliente(logged));
            userMenu.setHandler(4, () -> this.user_central_vendedor(logged));
            userMenu.setTitle("User");
            userMenu.run();
        } else{
            System.out.println("E-mail ainda não registado.");
        }
    }

    // menu do cliente
    private void user_central_cliente(Utilizador logged){
        NewMenu userMenu = new NewMenu(new String[]{
                "Encomendar", "Ver Artigos comprados", "Ver todas encomendas","Devolução"
        });

        userMenu.setHandler(1, () -> this.user_encomendar(logged));
        userMenu.setHandler(2, () -> this.user_bought(logged));
        userMenu.setHandler(3, () -> this.encomendas(logged));
        userMenu.setHandler(4, () -> this.devolucao(logged));
        userMenu.setTitle("Cliente");
        userMenu.run();
    }

    // mostrar as encomendas do user
    private void encomendas(Utilizador logged){
        System.out.println(logged.getEncomendas());
    }

    // pedir devolução numa encomenda
    private void devolucao(Utilizador logged){
        String d = "";
        while(true){
            System.out.println("Introduza o numero da encomenda: ");
            d = scin.nextLine();
            if(logged.getEncomendas().containsKey(d)) break;
            System.out.println("Esse de encomenda não existe! Cancelar devolução [y/n]?");
            if(scin.nextLine().contains("y")) return;
        }

        if(logged.getEncomendas().get(d).devolucao(this.getModel().now())){
            //remove outra vez o rev das transportadoras
            Map<String, Transportadoras> adicionadas = new HashMap<>();
            Map<String, Integer> quantidade = new HashMap<>();
            Map<String, Transportadoras> adicionadasp = new HashMap<>();
            Map<String, Integer> quantidadep = new HashMap<>();

            transportadoras_price(logged.getEncomendas().get(d), adicionadas, quantidade, adicionadasp, quantidadep);

            double rm = 0.0;

            for (Map.Entry<String, Transportadoras> entry : adicionadas.entrySet()){
                if(quantidade.get(entry.getKey()) <= 1){
                    rm += entry.getValue().getPrecoExp().getPequeno();
                    entry.getValue().setRev(entry.getValue().getRev() - entry.getValue().getPrecoExp().getPequeno());
                }else if(quantidade.get(entry.getKey()) <= 5){
                    rm += entry.getValue().getPrecoExp().getMedio();
                    entry.getValue().setRev(entry.getValue().getRev() - entry.getValue().getPrecoExp().getMedio());
                }else if(quantidade.get(entry.getKey()) > 5){
                    rm += entry.getValue().getPrecoExp().getGrande();
                    entry.getValue().setRev(entry.getValue().getRev() - entry.getValue().getPrecoExp().getGrande());
                }
            }

            for (Map.Entry<String, Transportadoras> entry : adicionadasp.entrySet()){
                if(quantidadep.get(entry.getKey()) <= 1){
                    rm += entry.getValue().getPrecoPremium().getPequeno();
                    entry.getValue().setRev(entry.getValue().getRev() - entry.getValue().getPrecoPremium().getPequeno());
                }else if(quantidadep.get(entry.getKey()) <= 5){
                    rm += entry.getValue().getPrecoPremium().getMedio();
                    entry.getValue().setRev(entry.getValue().getRev() - entry.getValue().getPrecoPremium().getMedio());
                }else if(quantidadep.get(entry.getKey()) > 5){
                    rm += entry.getValue().getPrecoPremium().getGrande();
                    entry.getValue().setRev(entry.getValue().getRev() - entry.getValue().getPrecoPremium().getGrande());
                }
            }
            //remover no file sales.txt
            String save = "saves/sales.txt";
            File file = new File(save);
            int mode;
            if(file.exists()){
                try{ //data, email, id do artigo
                    StringBuilder fline = new StringBuilder();
                    BufferedReader in = new BufferedReader(new FileReader(save));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while((line = in.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    in.close();
                    String read = sb.toString();
                    String[] lines = read.split("\n");
                    String[] sub;
                    for (String c : lines) {
                        sub = c.split(" ");
                        System.out.println(sub[0] + logged.getEncomendas().get(d).getData().toString());
                        if(!(sub[0].equals(logged.getEncomendas().get(d).getData().toString()))){
                            fline.append(c);
                            fline.append("\n");
                        } else{
                            for(Map.Entry<String, Artigo> entry : logged.getEncomendas().get(d).getArtigos().entrySet()){
                                if(sub[2].equals(entry.getValue().getID())){
                                    fline.append(sub[0]).append(" ").append(sub[1]).append(" ").append(sub[2]).append(" ").append(Integer.parseInt(sub[3]) - 1);
                                    fline.append("\n");
                                } else{
                                    fline.append(c);
                                    fline.append("\n");
                                }
                            }
                        }

                    }
                    this.getModel().writeTxt(save, String.valueOf(fline));
                } catch (IOException e){
                    System.err.println("Error opening save: " + e);
                }
            }

            //remover vendas
            Utilizador nuser;
            for (Map.Entry<String, Utilizador> entry : this.getModel().getUser().entrySet()) {
                nuser = entry.getValue();
                for (Map.Entry<String, Artigo> entry2 : nuser.getArtigos().entrySet()) {
                    for(Map.Entry<String, Artigo> entry3 : logged.getEncomendas().get(d).getArtigos().entrySet()){
                        if(entry3.getValue().getID().equals(entry2.getValue().getID())){
                            if(entry2.getValue().getClass().equals(Malas.class)){
                                Malas m = getMalaFromArtigo(entry2.getValue());
                                m.setSold(m.getSold() -1);
                                nuser.getArtigos().put(m.getID(), m);
                            } else if(entry2.getValue().getClass().equals(Sapatilhas.class)){
                                Sapatilhas m = getShoeFromArtigo(entry2.getValue());
                                m.setSold(m.getSold() -1);
                                nuser.getArtigos().put(m.getID(), m);
                            }else if(entry2.getValue().getClass().equals(TShirt.class)){
                                TShirt m = getTshirtFromArtigo(entry2.getValue());
                                m.setSold(m.getSold() -1);
                                nuser.getArtigos().put(m.getID(), m);
                            }
                            rm += entry2.getValue().getPreco();
                        }
                    }

                }
                this.getModel().getUser().put(entry.getKey(), nuser);
            }
            this.getModel().setRev(this.getModel().getRev() - (rm *this.getModel().getVintagecut()));
            System.out.println("Devolução pedida com sucesso!");
        } else{
            System.out.println("Dias para devolução não estão dentro dos 14 dias após compra.");
        }
    }

    // menu do vendedor
    private void user_central_vendedor(Utilizador logged){

        NewMenu userMenu = new NewMenu(new String[]{
                "Criar novo artigo", "Publicar/Privar artigo",
                "Remover artigo", "Ver receita", "Ver artigos criados",
                "Ver Artigos vendidos", "Ver Artigos á venda",
                "Alterar configurações de um artigo", "Duplicar artigo",
                "Ver encomendas enviadas"
        });

        userMenu.setHandler(1, () -> this.user_new_artigo(logged));
        userMenu.setHandler(2, () -> this.user_publish_artigo(logged));
        userMenu.setHandler(3, () -> this.user_rm_artigo(logged));
        userMenu.setHandler(4, () -> this.user_receita(logged));
        userMenu.setHandler(5, () ->this.user_artigo_created(logged));
        userMenu.setHandler(6, () ->this.user_sold(logged));
        userMenu.setHandler(7, () -> this.user_selling(logged));
        userMenu.setHandler(8, () -> this.user_artigo_config(logged));
        userMenu.setHandler(9, () -> this.user_artigo_clone(logged));
        userMenu.setHandler(10, () -> this.user_sent(logged));
        userMenu.setTitle("Vendedor Menu");
        userMenu.run();
    }

    // menu das encomendas ja enviadas
    private void user_sent(Utilizador logged){

        NewMenu userMenu = new NewMenu(new String[]{
                "Encomendas que a transportadora já enviou",
                "Só entregues à transportadora"
        });

        userMenu.setHandler(1, () -> this.sent(2, logged));
        userMenu.setHandler(2, () -> this.sent(1, logged));
        userMenu.setTitle("Encomendas dos clientes");
        userMenu.run();

    }

    // mostra os enviados, 1 para os mandados so para a transportadora, 2 transportadora ja enviou
    private void sent(int estado, Utilizador logged){
        int equals;
        for(Map.Entry<String, Utilizador> entry : this.getModel().getUser().entrySet()){
            for(Map.Entry<String, Encomendas> entry2 : entry.getValue().getEncomendas().entrySet()){
                if(entry2.getValue().getEstado() == estado){
                    equals = 0;
                    for(Map.Entry<String, Artigo> entry3 : entry2.getValue().getArtigos().entrySet()){
                        for(Map.Entry<String, Artigo> entry4 : logged.getArtigos().entrySet()){
                            if(entry3.getValue().equals(entry4.getValue()) && entry4.getValue().getSold() > 0){
                                equals +=1;
                            }
                        }
                    }
                    if(equals > 0){
                        System.out.println("To: " + entry.getValue().getEmail() + ": " + entry2.getValue());
                    }
                }
            }
        }
    }

    // duplicar um artigo
    private void user_artigo_clone(Utilizador logged){
        System.out.println("Introduza o id do artigo a duplicar: ");
        String id = scin.nextLine(), cancel = "";
        while(!(logged.getArtigos().containsKey(id))){
            System.out.println("Não existe artigo com id " + id + " na sua conta.");
            System.out.println("Cancelar a mudança [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("id: ");
            id = scin.nextLine();
        }
        if(cancel.contains("y")) return;

        Artigo nArtigo = logged.getArtigos().get(id).clone();

        while(this.getModel().getArtigos().containsKey(nArtigo.getID())){
            nArtigo.setID(nArtigo.generateID());
        }
        System.out.println("Id do novo artigo: " + nArtigo.getID());

        logged.addArtigo(nArtigo);

    }

    // ver artigos que o user criou, do passado ate ao presente
    private void user_artigo_created(Utilizador logged){
        for(Map.Entry<String, Artigo> entry : logged.getArtigos().entrySet()) {
            if (!this.getModel().now().isBefore(entry.getValue().getBorn())) {
                System.out.println(entry.getValue());
            }
        }
    }

    // menu de para mudar variaveis em artigos
    private void user_artigo_config(Utilizador logged){
        System.out.println("Introduza o id do artigo a editar: ");
        String id = scin.nextLine();

        if(logged.getArtigos().containsKey(id)){
            NewMenu artigo_Menu = new NewMenu(new String[]{
                    "Mudar estado", "Mudar descricao",
                    "Mudar Marca", "Mudar Numero de Donos",
                    "Mudar preco base","Mudar colecao","Mudar transportadora", "Aumentar Stock"

            });
            Artigo artigo = logged.getArtigos().get(id);
            artigo_Menu.setHandler(1, () -> this.artigo_estado(artigo, logged));
            artigo_Menu.setHandler(2, () -> this.artigo_descricao(artigo));
            artigo_Menu.setHandler(3, () -> this.artigo_brand(artigo));
            artigo_Menu.setHandler(4, () -> this.artigo_NDonos(artigo, logged));
            artigo_Menu.setHandler(5, () -> this.artigo_precobase(artigo, logged));
            artigo_Menu.setHandler(6, () -> this.artigo_colecao(artigo, logged));
            artigo_Menu.setHandler(7, () -> this.artigo_transportadora(artigo));
            artigo_Menu.setHandler(8, () -> this.artigo_stock(artigo));

            artigo_Menu.setPreCondition(1, ()-> artigo.getNumeroDonos() > 0);
            artigo_Menu.setTitle("Artigo Menu");
            artigo_Menu.run();
        } else{
            System.out.println("Artigo com id " + id + " não existe.");
        }
    }

    //aumenta o stock
    private void artigo_stock(Artigo artigo){

        while(true){
            System.out.println("Novo stock");
            String stock = scin.nextLine();
            try{
                int st = Integer.parseInt(stock);
                if(st > 0){
                    artigo.setStock(st);
                    break;
                } else{
                    System.out.println("Número não válido. cancelar operação [y/n]? ");
                    if(scin.nextLine().contains("y")) return;
                }
            } catch (NumberFormatException e){

            }
        }

    }

    // mudar estado do artigo
    private void artigo_estado(Artigo artigo, Utilizador logged) {
        String estado="", cancel="";
        System.out.println("Introduza o estado (Pouco usado, Usado, Muito usado):");
        estado = scin.nextLine();
        while(!( estado.equals("Pouco usado") ||  estado.equals("Usado") ||  estado.equals("Muito usado"))){
            System.out.println("Tipo de estado invalido!");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Introduza o estado (Pouco usado, Usado, Muito usado):");
            estado = scin.nextLine();
        }
        if(cancel.contains("y")) return;
        artigo.setEstado(estado);
        update_artigo(artigo, logged);
    }

    // mudar descriçao do artigo
    private void artigo_descricao(Artigo artigo) {
        System.out.println("Nova descricao:");
        String descricao= scin.nextLine();
        artigo.setDescricao(descricao);
    }

    // mudar marca do artigo
    private void artigo_brand(Artigo artigo){
        System.out.println("Nova marca:");
        String brand= scin.nextLine();
        artigo.setBrand(brand);
    }

    // mudar numero de donos do artigo
    private void artigo_NDonos(Artigo artigo, Utilizador logged){
        int numerodonos=0;
        System.out.println("Introduza o numero de donos: ");
        String nd = scin.nextLine(), cancel="";
        numerodonos = Integer.parseInt(nd);
        while(numerodonos < 0){
            System.out.println("Numero de donos invalido");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Introduza o numero de donos:");
            nd = scin.nextLine();
            numerodonos=Integer.parseInt(nd);
        }
        if(cancel.contains("y")) return;
        artigo.setNumeroDonos(numerodonos);

        if(numerodonos != 0){
            artigo_estado(artigo, logged);
        } else{
            artigo.setEstado("Novo");
        }
    }

    // mudar preço base do artigo
    private void artigo_precobase(Artigo artigo, Utilizador logged){
        System.out.println("Novo preco base:");
        double val = Integer.parseInt(scin.nextLine());
        String cancel="";
        while(val < 0){
            System.out.println("Preço invalido");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Introduza o preço:");
            val = Integer.parseInt(scin.nextLine());
        }
        if(cancel.contains("y")) return;

        artigo.setPrecobase(val);
        update_artigo(artigo, logged);
    }

    // mudar coleçao do artigo
    private void artigo_colecao(Artigo artigo, Utilizador logged){
        System.out.println("Nome da nova coleção:");
        String id = scin.nextLine(),cancel="";
        while(!(this.getModel().getColecao().containsKey(id))){
            System.out.println("Não existe coleção com id/nome: " + id);
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Introduza o nome da colecao: ");
            id = scin.nextLine();
        }

        artigo.setColecao(this.getModel().getColecao().get(id));
        update_artigo(artigo, logged);
    }

    // mudar transportadora do artigo
    private void artigo_transportadora(Artigo artigo){
        System.out.println("Id da nova transportadora:");
        String idt = scin.nextLine(),cancel="";
        boolean loop;
        loop = !(this.getModel().getTransportadora().containsKey(idt));
        if(!loop) loop = artigo.isPremium() && !this.getModel().getTransportadora().get(idt).getPremium();
        while(loop){
            if(!(this.getModel().getTransportadora().containsKey(idt))) System.out.println(
                    "Não existe transportadora com id: " + idt);
            else if(artigo.isPremium() && !this.getModel().getTransportadora().get(idt).getPremium()) System.out.println(
                    "Essa transportadora não tem expedição premium!");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Introduza o id da transportadora: ");
            idt = scin.nextLine();
            loop = !(this.getModel().getTransportadora().containsKey(idt));
            if(!loop) loop = artigo.isPremium() && !this.getModel().getTransportadora().get(idt).getPremium();
        }

        artigo.setTransportadoras(this.getModel().getTransportadora().get(idt));
    }

    // mostra os detalhes do usuario
    private void user_details(Utilizador logged){
        String print = "";
        if(!(logged.getRevenue() == 0.0)) print += "Receita: " + logged.getRevenue() + "\n";
        print += "Artigos a venda: ";
        for(Map.Entry<String, Artigo> entry : logged.getArtigos().entrySet()){
            if(entry.getValue().isPublicado()){
                print += entry.getValue();
            }
        }
        print += "Vendas efetuadas: ";
        for(Map.Entry<String, Artigo> entry : logged.getArtigos().entrySet()){
            if(entry.getValue().getSold() > 0){
                print += this.getModel().getSold() + "\n";
            }
        }
        if(logged.getArtigos().size() > 0) print += "Artigos á venda: " + logged.getArtigos() + "\n";
        if(logged.getEncomendas().size() > 0) print += "Encomendas Realizadas: " + logged.getEncomendas();
        System.out.println("Email: " + logged.getEmail() + "\n"
                + "Nome: " + logged.getNome() + "\n"
                + "Morada: " + logged.getMorada() + "\n"
                + "Nif: " + logged.getNif() + "\n"
                + "User ID: " + logged.getID() + "\n"
                + print
        );
    }

    // menu para mudar a info do user
    private void user_change_config(Utilizador logged){
        NewMenu config_user_Menu = new NewMenu(new String[]{
                "Mudar email" , "Mudar nome", "Mudar morada", "Mudar nif"
        });

        config_user_Menu.setHandler(1, ()-> this.email_user(logged));
        config_user_Menu.setHandler(2, ()-> this.nome_user(logged));
        config_user_Menu.setHandler(3, ()-> this.morada_user(logged));
        config_user_Menu.setHandler(4, ()-> this.nif_user(logged));
        config_user_Menu.setTitle("User Config Menu");
        config_user_Menu.run();
    }

    // mudar email do user
    private void email_user(Utilizador logged){
        System.out.println("Introduza o novo email:");
        String email = scin.nextLine(), cancel = "";
        while(!(email.contains("@")) || this.getModel().existsEmail(email)){
            if(!(email.contains("@"))){
                System.out.println("E-mail Inválido! (não contém @)");
            }if(this.getModel().existsEmail(email)){
                System.out.println("Este e-mail já está registado!");
            }
            System.out.println("Cancelar a mudança [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("E-mail: ");
            email = scin.nextLine();
        }
        if(cancel.contains("y")) return;
        logged.setEmail(email);
        System.out.println("Novo e-mail: " + logged.getEmail());
    }

    // mudar nome do user
    private void nome_user(Utilizador logged){
        System.out.println("Introduza novo nome:");
        String nome = scin.nextLine();
        logged.setNome(nome);
        System.out.println("Novo nome: " + logged.getNome());
    }

    // muda a morada do user
    private void morada_user(Utilizador logged){
        System.out.println("Introduza nova morada:");
        String m = scin.nextLine();
        logged.setMorada(m);
        System.out.println("Nova morada: " + logged.getMorada());
    }

    //muda o nif do user
    private void nif_user(Utilizador logged){
        System.out.println("Introduza novo nif:");
        String nif = scin.nextLine(), cancel = "";
        while(nif.length() != 9){
            System.out.println("Nif Inválido!");
            System.out.println("Cancelar a mudança [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Nif (123456789): ");
            nif = scin.nextLine();
        }
        if(cancel.contains("y")) return;
        logged.setNif(nif);
        System.out.println("Novo nif: " + logged.getNif());
    }

    // menu para criar novo artigo
    private void user_new_artigo(Utilizador logged){
        NewMenu config_user_Menu = new NewMenu(new String[]{
                "Mala" , "Tshirt", "Sapatilha"
        });
        config_user_Menu.setHandler(1, ()-> this.user_mala(logged));
        config_user_Menu.setHandler(2, ()-> this.user_tshirt(logged));
        config_user_Menu.setHandler(3, ()-> this.user_sapatilha(logged));
        config_user_Menu.setTitle("Type Artigo Menu");
        config_user_Menu.run();
    }

    // criar mala
    private void user_mala(Utilizador logged){
        Malas mala=new Malas();
        System.out.println("Introduza o tamanho da Mala (Pequeno, Medio, Grande): ");
        System.out.println("Se as medidas (0,0 -> 10,0 x 0,0 -> 15,0 x 0,0 -> 10,0) é Pequeno");
        System.out.println("Se as medidas (10,0 -> 15,0 x 10,0-> 15,0 x 10,0 -> 15,0) é Medio");
        System.out.println("Se as medidas (15,0 -> 20,0 x 15,0 -> 25,0 x 15,0 -> 20,0) é Grande");
        String tamanho=scin.nextLine(), cancel ="";
        while(!(tamanho.equals("Pequeno") || tamanho.equals("Medio") || tamanho.equals("Grande"))){
            System.out.println("Tamanho Inválido");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Tamanho da mala (Pequeno, Medio, Grande):");
            tamanho = scin.nextLine();
        }
        if(cancel.contains("y")) return;
        mala.setTamanho(tamanho);

        System.out.println("Introduza o material:");
        String material=scin.nextLine();
        mala.setMaterial(material);

        String bool;
        System.out.println("Deseja ativar premium [y/n]:");
        bool=scin.nextLine();
        int p= 0;
        if(bool.contains("y")){
            p = 1;
            mala.ativaPremium();
            mala.setDataPremium(this.getModel().now());

            System.out.println("Valorização do premium ao ano (0-100): ");
            double val = Integer.parseInt(scin.nextLine());

            while(val < 0 || val > 100){
                System.out.println("A valorização só pode ser entre 0% e 100%");
                System.out.println("Cancelar a adição [y/n]?");
                cancel = scin.nextLine();
                if(cancel.contains("y")) break;
                System.out.println("Valorização: ");
                val = Integer.parseInt(scin.nextLine());
            }
            if(cancel.contains("y")) return;
            mala.setValorizacao(val/100);

            mala.setDataPremium(this.getModel().now());
        }

        Artigo artigo = common_artigo(p);
        if(artigo.getTransportadoras() == null){
            return;
        }

        mala.setNumeroDonos(artigo.getNumeroDonos());
        mala.setEstado(artigo.getEstado());
        mala.setBrand(artigo.getBrand());
        mala.setPrecobase(artigo.getPrecobase());
        mala.setDescricao(artigo.getDescricao());
        mala.setTransportadoras(artigo.getTransportadoras());
        mala.setColecao(artigo.getColecao());
        mala.calcPreco(this.getModel().now());

        System.out.println("Tamanho da mala: " + mala.getTamanho());
        System.out.println("Material da mala: " + mala.getMaterial());
        if(mala.isPremium()) System.out.println("Valorização ao ano: " + mala.getValorizacao()*100 + "%");

        System.out.println("Numero de donos: " + mala.getNumeroDonos());
        System.out.println("Estado: " + mala.getEstado());
        System.out.println("Marca: " + mala.getBrand());
        System.out.println("Descricao do artigo: "+ mala.getDescricao());

        System.out.println("Preco base: " + mala.getPrecobase());
        System.out.println("Preco após cálculos: " + mala.getPreco());

        while(this.getModel().getArtigos().containsKey(mala.getID())){
            mala.setID(mala.generateID());
        }
        System.out.println("Id do artigo: " + mala.getID());
        System.out.println("Stock: " + mala.getStock());

        mala.setBorn(this.getModel().now());
        logged.addArtigo(mala);
    }

    // criar tshirt
    private void user_tshirt(Utilizador logged){ //padrao
        TShirt tshirt = new TShirt();
        System.out.println("Introduza o tamanho da tshirt (S, M, L, XL, XXL):");
        String tamanho=scin.nextLine(), cancel ="";
        while(!(tamanho.equals("S") || tamanho.equals("M") || tamanho.equals("L") || tamanho.equals("XL") || tamanho.equals("XXL"))){
            System.out.println("Tamanho Inválido");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Tamanho (S, M, L, XL, XXL):");
            tamanho = scin.nextLine();
        }
        if(cancel.contains("y")) return;
        tshirt.setTamanho(tamanho);


        System.out.println("Introduza o padrao da TShirt (liso, riscas, palmeiras): ");
        String padrao=scin.nextLine();
        while(!(padrao.equals("palmeiras") || padrao.equals("riscas") || padrao.equals("liso"))){
            System.out.println("Padrão Inválido");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Padrao: (liso, riscas, palmeiras: ");
            padrao = scin.nextLine();
        }
        if(cancel.contains("y")) return;
        tshirt.setPadrao(padrao);

        Artigo artigo = common_artigo(0);
        if(artigo.getTransportadoras() == null){
            return;
        }

        tshirt.setNumeroDonos(artigo.getNumeroDonos());
        tshirt.setEstado(artigo.getEstado());
        tshirt.setBrand(artigo.getBrand());
        tshirt.setPrecobase(artigo.getPrecobase());
        tshirt.setDescricao(artigo.getDescricao());
        tshirt.setTransportadoras(artigo.getTransportadoras());
        tshirt.setColecao(artigo.getColecao());
        tshirt.calcPreco();

        System.out.println("Tamanho da tshirt: " + tshirt.getTamanho());
        System.out.println("Padrao da tshirt: " + tshirt.getPadrao());

        System.out.println("Numero de donos: " + tshirt.getNumeroDonos());
        System.out.println("Estado: " + tshirt.getEstado());
        System.out.println("Marca: " + tshirt.getBrand());
        System.out.println("Descricao do artigo: "+ tshirt.getDescricao());

        System.out.println("Preco base: " + tshirt.getPrecobase());
        System.out.println("Preco após cálculos: " + tshirt.getPreco());
        System.out.println("Stock: " + tshirt.getStock());

        while(this.getModel().getArtigos().containsKey(tshirt.getID())){
            tshirt.setID(tshirt.generateID());
        }
        System.out.println("Id do artigo: " + tshirt.getID());

        tshirt.setBorn(this.getModel().now());
        logged.addArtigo(tshirt);
    }

    // criar sapatilha
    private void user_sapatilha(Utilizador logged){ //atacadores, cor

        Sapatilhas sapatilha=new Sapatilhas();
        System.out.println("Introduza o tamanho das sapatilhas (15...50):");
        String tam=scin.nextLine(), cancel= "", nd;
        int tamanho = 0;
        try{
            tamanho=Integer.parseInt(tam);
        } catch (NumberFormatException e){

        }
        while(tamanho < 15 || tamanho > 50){
            System.out.println("Tamanho Inválido");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Introduza o tamanho (15...50):");
            nd = scin.nextLine();
            try{
                tamanho=Integer.parseInt(nd);
            } catch (NumberFormatException e){

            }
        }
        if(cancel.contains("y")) return;

        sapatilha.setTamanho(tamanho);


        System.out.println("Atacador ou fio?");
        String ata=scin.nextLine();
        while(!(ata.equals("Atacador") || ata.equals("fio") || ata.equals("atacador") || ata.equals("Fio"))){
            System.out.println("Isso não é fio ou atacador");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Atacador ou fio?");
            ata = scin.nextLine();
        }
        if(cancel.contains("y")) return;
        sapatilha.setAtacadores(ata.equals("Atacador"));

        System.out.println("Introduza a cor:");
        String cor=scin.nextLine();
        sapatilha.setCor(cor);

        String bool;
        System.out.println("Deseja ativar premium [y/n]:");
        bool=scin.nextLine();
        int p = 0;
        if(bool.contains("y")){
            p=1;
            sapatilha.ativaPremium();
            sapatilha.setDataPremium(this.getModel().now());
        }

        Artigo artigo = common_artigo(p);
        if(artigo.getTransportadoras() == null){
            return;
        }

        sapatilha.setNumeroDonos(artigo.getNumeroDonos());
        sapatilha.setEstado(artigo.getEstado());
        sapatilha.setBrand(artigo.getBrand());
        sapatilha.setPrecobase(artigo.getPrecobase());
        sapatilha.setDescricao(artigo.getDescricao());
        sapatilha.setTransportadoras(artigo.getTransportadoras());
        sapatilha.setColecao(artigo.getColecao());
        sapatilha.calcPreco(this.getModel().now());

        System.out.println("Cor da sapatilha: " + sapatilha.getCor());
        System.out.println("Tamanho da sapatilha: " + sapatilha.getTamanho());
        System.out.println("Atacadores da sapatilha: " + sapatilha.getAtacadores());

        System.out.println("Numero de donos: " + sapatilha.getNumeroDonos());
        System.out.println("Estado: " + sapatilha.getEstado());
        System.out.println("Marca: " + sapatilha.getBrand());

        System.out.println("Descricao do artigo: "+ sapatilha.getDescricao());
        System.out.println("Preco base: " + sapatilha.getPrecobase());
        System.out.println("Preco após cálculos: " + sapatilha.getPreco());
        System.out.println("Stock: " + sapatilha.getStock());

        while(this.getModel().getArtigos().containsKey(sapatilha.getID())){
            sapatilha.setID(sapatilha.generateID());
        }
        System.out.println("Id do artigo: " + sapatilha.getID());

        sapatilha.setBorn(this.getModel().now());
        logged.addArtigo(sapatilha);
    }

    // construtor comum do artigo
    public Artigo common_artigo(int p){
        Artigo artigo=new Artigo();
        String cancel="";
        int numerodonos=0;
        System.out.println("Introduza o numero de donos: ");
        String nd = scin.nextLine();
        try{
            numerodonos=Integer.parseInt(nd);
        } catch (NumberFormatException e){

        }
        while(numerodonos < 0){
            System.out.println("Numero de donos invalidos");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Introduza o numero de donos:");
            nd = scin.nextLine();
            try{
                numerodonos=Integer.parseInt(nd);
            } catch (NumberFormatException e){

            }

        }
        if(cancel.contains("y")) return new Artigo();
        artigo.setNumeroDonos(numerodonos);

        if(numerodonos != 0){
            String estado="";
            System.out.println("Introduza o estado (Pouco usado, Usado, Muito usado):");
            estado = scin.nextLine();
            while(!( estado.equals("Pouco usado") ||  estado.equals("Usado") ||  estado.equals("Muito usado"))){
                System.out.println("Tipo de estado invalido!");
                System.out.println("Cancelar a adição [y/n]?");
                cancel = scin.nextLine();
                if(cancel.contains("y")) break;
                System.out.println("Introduza o estado (Pouco usado, Usado, Muito usado):");
                estado = scin.nextLine();
            }
            if(cancel.contains("y")) return new Artigo();
            artigo.setEstado(estado);
        } else{
            artigo.setEstado("Novo");
        }

        System.out.println("Introduza a marca do artigo: ");
        String brand=scin.nextLine();
        artigo.setBrand(brand);

        double precobase;
        String price = "";
        while(true){
            System.out.println("Introduza o preco base (sem calculos): ");
            price = scin.nextLine();
            try{
                precobase=Integer.parseInt(price);
                if(precobase < 0){
                    System.out.println("Preço não válido. Cancelar a adição [y/n]?");
                    cancel = scin.nextLine();
                    if(cancel.contains("y")) return new Artigo();
                } else{
                    break;
                }
            } catch (NumberFormatException ignored){
                System.err.println("Numero não válido");
            }
        }

        artigo.setPrecobase(precobase);

        System.out.println("Descricao do artigo: ");
        String descricao = scin.nextLine();
        artigo.setDescricao(descricao);
        int stock = 0;
        while(true){
            System.out.println("Stock do artigo: ");
            price = scin.nextLine();
            try{
                stock=Integer.parseInt(price);
                if(stock < 0){
                    System.out.println("Stock não válido. Cancelar a adição [y/n]?");
                    cancel = scin.nextLine();
                    if(cancel.contains("y")) return new Artigo();
                } else{
                    break;
                }
            } catch (NumberFormatException ignored){
                System.err.println("Numero não válido");
            }
        }
        artigo.setStock(stock);


        System.out.println("Nome da colecao do artigo:");
        String id = scin.nextLine();
        while(!(this.getModel().getColecao().containsKey(id))){
            System.out.println("Não existe coleção com id/nome: " + id);
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Introduza um Id/nome da colecao: ");
            id = scin.nextLine();
        }
        if(cancel.contains("y")) return new Artigo();

        artigo.setColecao(this.getModel().getColecao().get(id));

        System.out.println("Id da transportadora:");
        String idt = scin.nextLine();
        boolean loop;
        loop = !(this.getModel().getTransportadora().containsKey(idt));
        if(!loop) loop = p == 1 && !this.getModel().getTransportadora().get(idt).getPremium();
        while(loop){
            if(!(this.getModel().getTransportadora().containsKey(idt))) System.out.println("Não existe transportadora com id: " + idt);
            else if(p == 1 && !this.getModel().getTransportadora().get(idt).getPremium()) System.out.println("Essa transportadora não tem expedição premium!");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Introduza o id da transportadora: ");
            idt = scin.nextLine();
            loop = !(this.getModel().getTransportadora().containsKey(idt));
            if(!loop) loop = p == 1 && !this.getModel().getTransportadora().get(idt).getPremium();
        }
        artigo.setTransportadoras(this.getModel().getTransportadora().get(idt));

        return artigo;
    }

    // publica um artigo
    private void user_publish_artigo(Utilizador logged){
        System.out.println("Insira o ID do artigo a publicar: ");
        String id = scin.nextLine();
        if(logged.getArtigos().containsKey(id)){
            if(logged.getArtigos().get(id).getSold() == logged.getArtigos().get(id).getStock()){
                if(logged.getArtigos().get(id).isPublicado()){
                    logged.getArtigos().get(id).privar();
                    System.out.println("Privado com sucesso!");
                } else {
                    logged.getArtigos().get(id).publicar();
                    System.out.println("Publicado com sucesso!");
                }
            } else{
                System.out.println("Sem stock!");
            }
        } else{
            System.out.println("Artigo " + id + " não existe");
        }
    }

    // remove um artigo
    private void user_rm_artigo(Utilizador logged){

        System.out.println("Insira o ID do artigo a remover: ");
        String id = scin.nextLine();
        if(logged.getArtigos().containsKey(id)){
            System.out.println("Tem a certeza que quer remover o artigo [y/n]?");
            String clean = scin.nextLine();
            if(clean.contains("y")){
                logged.getArtigos().remove(id);
                System.out.println("Artigo removido com sucesso!");
            }else{
                System.out.println("Cancelado com successo!");
            }
        } else{
            System.out.println("Artigo " + id + " não existe");
        }
    }

    // menu da encomenda do user
    private void user_encomendar(Utilizador logged){
        Encomendas current = new Encomendas();
        for(Map.Entry<String, Encomendas> entry : logged.getEncomendas().entrySet()){
            if(entry.getValue().getEstado() == 0){
                current = entry.getValue();
                logged.getEncomendas().remove(entry.getKey());
            }
        }

        NewMenu user_encomendar = new NewMenu(new String[]{
                "Comprar", "Adicionar artigo" , "Remover artigo", "Ver carrinho", "Limpar Carrinho",
                "Ver cardapio"
        });

        Encomendas finalCurrent = current;
        AtomicInteger c = new AtomicInteger();
        user_encomendar.setHandler(1, ()-> c.set(this.encomenda_comprar(finalCurrent, logged)));
        user_encomendar.setHandler(2, ()-> this.encomenda_add(finalCurrent));
        user_encomendar.setHandler(3, ()-> this.encomenda_rm(finalCurrent));
        user_encomendar.setHandler(4, ()-> this.encomenda_see(finalCurrent));
        user_encomendar.setHandler(5, ()-> this.encomenda_clean(finalCurrent));
        user_encomendar.setHandler(6, this::encomenda_cardapio);

        user_encomendar.setPreCondition(1, ()-> finalCurrent.getArtigos().size() > 0);
        user_encomendar.setPreCondition(3, ()-> finalCurrent.getArtigos().size() > 0);
        user_encomendar.setPreCondition(5, ()-> finalCurrent.getArtigos().size() > 0);
        user_encomendar.setPreCondition(6, ()-> this.getModel().getArtigos().size() > 0);
        user_encomendar.setTitle("Encomenda");
        user_encomendar.run();

        if(c.get()!=1){
            logged.adicionarEncomenda(finalCurrent);
        }
    }

    // cardapio (todos artigos publicos)
    public void encomenda_cardapio(){
        System.out.println(this.getModel().getCardapio(this.getModel().now()));
    }

    // comprar a encomenda
    private int encomenda_comprar(Encomendas current, Utilizador logged){

        Map<String, Transportadoras> adicionadas = new HashMap<>();
        Map<String, Integer> quantidade = new HashMap<>();
        Map<String, Transportadoras> adicionadasp = new HashMap<>();
        Map<String, Integer> quantidadep = new HashMap<>();

        transportadoras_price(current, adicionadas, quantidade, adicionadasp, quantidadep);
        double transportadoras = 0.0;

        for (Map.Entry<String, Transportadoras> entry : adicionadas.entrySet()){
            if(quantidade.get(entry.getKey()) <= 1){
                transportadoras += entry.getValue().getPrecoExp().getPequeno();
                entry.getValue().setRev(entry.getValue().getPrecoExp().getPequeno()
                        + entry.getValue().getRev());
            }else if(quantidade.get(entry.getKey()) <= 5){
                transportadoras += entry.getValue().getPrecoExp().getMedio();
                entry.getValue().setRev(entry.getValue().getPrecoExp().getMedio()
                        + entry.getValue().getRev());
            }else if(quantidade.get(entry.getKey()) > 5){
                transportadoras += entry.getValue().getPrecoExp().getGrande();
                entry.getValue().setRev(entry.getValue().getPrecoExp().getGrande()
                        + entry.getValue().getRev());
            }
        }

        for (Map.Entry<String, Transportadoras> entry : adicionadasp.entrySet()){
            if(quantidadep.get(entry.getKey()) <= 1){
                transportadoras += entry.getValue().getPrecoExp().getPequeno();
                entry.getValue().setRev(entry.getValue().getPrecoPremium().getPequeno()
                        + entry.getValue().getRev());
            }else if(quantidadep.get(entry.getKey()) <= 5){
                transportadoras += entry.getValue().getPrecoExp().getMedio();
                entry.getValue().setRev(entry.getValue().getPrecoPremium().getMedio()
                        + entry.getValue().getRev());
            }else if(quantidadep.get(entry.getKey()) > 5){
                transportadoras += entry.getValue().getPrecoExp().getGrande();
                entry.getValue().setRev(entry.getValue().getPrecoPremium().getGrande()
                        + entry.getValue().getRev());
            }
        }

        double preco = 0.0;
        for (Map.Entry<String, Artigo> entry : current.getArtigos().entrySet()) {
            if(entry.getValue().getPreco() != -1.0){
                preco += entry.getValue().getPreco();
            }
        }

        this.getModel().setRev(this.getModel().getRev() + ((preco + transportadoras) * this.getModel().getVintagecut()));

        current.enviar(this.getModel().now());
        Utilizador nuser;
        for (Map.Entry<String, Utilizador> entry : this.getModel().getUser().entrySet()) {
            nuser = entry.getValue();
            for (Map.Entry<String, Artigo> entry2 : nuser.getArtigos().entrySet()) {
                for(Map.Entry<String, Artigo> entry3 : current.getArtigos().entrySet()){
                    if(entry3.getValue().getID().equals(entry2.getValue().getID())){
                        if(entry2.getValue().getClass().equals(Malas.class)){
                            Malas m = getMalaFromArtigo(entry2.getValue());
                            m.setSold(m.getSold() + 1);
                            nuser.getArtigos().put(m.getID(), m);
                        } else if(entry2.getValue().getClass().equals(Sapatilhas.class)){
                            Sapatilhas m = getShoeFromArtigo(entry2.getValue());
                            m.setSold(m.getSold() + 1);
                            nuser.getArtigos().put(m.getID(), m);
                        }else if(entry2.getValue().getClass().equals(TShirt.class)){
                            TShirt m = getTshirtFromArtigo(entry2.getValue());
                            m.setSold(m.getSold() + 1);
                            nuser.getArtigos().put(m.getID(), m);
                        }
                    }
                }

            }
            this.getModel().getUser().put(entry.getKey(), nuser);
        }
        logged.adicionarEncomenda(current.clone());
        current.setEstado(0);
        current.setArtigos(new HashMap<>());
        current.setData(null);
        current.setDimensao();
        current.setDevolucao(14);
        updates();
        System.out.println("Obrigado e volte sempre!");
        return 1;
    }

    // adicionar um produto na encomenda
    private void encomenda_add(Encomendas current){
        System.out.println("Id do artigo a adicionar: ");
        String id = scin.nextLine();

        if(this.getModel().getArtigos().containsKey(id)){
            if(this.getModel().getArtigos().get(id).isPublicado()){
                current.addArtigo(this.getModel().getArtigos().get(id));
            } else{
                System.out.println("Artigo com id: " + id + " não está disponivel para venda.");
            }
        } else{
            System.out.println("Artigo com id: " + id + " não existe.");
        }
    }

    // remover um produto na encomenda
    private void encomenda_rm(Encomendas current){
        System.out.println("Id do artigo a remover: ");
        String id = scin.nextLine();

        if(current.getArtigos().containsKey(id)){
            System.out.println("Tem a certeza que quer remover o artigo do carrinho [y/n]?");
            String clean = scin.nextLine();

            if(clean.contains("y")){
                current.getArtigos().remove(id);
                System.out.println("Limpado com successo!");
            }else{
                System.out.println("Cancelado com successo!");
            }
        } else{
            System.out.println("Artigo com id: " + id + " não existe");
        }
    }

    // analisa o numero de artigos por transportadora, por ex: se ha 2 transportadoras e 4 artigos, sendo 1 associado a uma e 3 a outra:
    // o preço das tranportadoras vai ser o pequeno da 1 + medio do 2.
    // aplicar a lógica de cima com premium também
    private void transportadoras_price(Encomendas current,
                                       Map<String, Transportadoras> adicionadas, Map<String, Integer> quantidade,
                                       Map<String, Transportadoras> adicionadasp, Map<String, Integer> quantidadep){
        boolean equal;
        for (Map.Entry<String, Artigo> entry : current.getArtigos().entrySet()) {
            if(entry.getValue().isPremium()){
                if(adicionadasp.size() == 0){
                    quantidadep.put("0", 1);
                    adicionadasp.put("0", entry.getValue().getTransportadoras());
                } else{
                    equal = false;
                    for (Map.Entry<String, Transportadoras> entry2 : adicionadasp.entrySet()){
                        if(entry.getValue().getTransportadoras().equals(entry2.getValue())){
                            quantidadep.put(entry2.getKey(), quantidadep.get(entry2.getKey()) + 1);
                            equal = true;
                        }
                    }
                    if(!equal){
                        quantidadep.put(Integer.toString(adicionadasp.size()), 1);
                        adicionadasp.put(Integer.toString(adicionadasp.size()), entry.getValue().getTransportadoras());
                    }
                }
            } else{
                if(adicionadas.size() == 0){
                    quantidade.put("0", 1);
                    adicionadas.put("0", entry.getValue().getTransportadoras());
                } else{
                    equal = false;
                    for (Map.Entry<String, Transportadoras> entry2 : adicionadas.entrySet()){
                        if(entry.getValue().getTransportadoras().equals(entry2.getValue())){
                            quantidade.put(entry2.getKey(), quantidade.get(entry2.getKey()) + 1);
                            equal = true;
                        }
                    }
                    if(!equal){
                        quantidade.put(Integer.toString(adicionadas.size()), 1);
                        adicionadas.put(Integer.toString(adicionadas.size()), entry.getValue().getTransportadoras());
                    }
                }
            }
        }
    }

    // ver produtos + preço da encomenda
    private void encomenda_see(Encomendas current){
        Map<String, Transportadoras> adicionadas = new HashMap<>();
        Map<String, Integer> quantidade = new HashMap<>();
        Map<String, Transportadoras> adicionadasp = new HashMap<>();
        Map<String, Integer> quantidadep = new HashMap<>();

        transportadoras_price(current, adicionadas, quantidade, adicionadasp, quantidadep);

        double transportadoras = 0.0;

        for (Map.Entry<String, Transportadoras> entry : adicionadas.entrySet()){
            if(quantidade.get(entry.getKey()) <= 1){
                transportadoras += entry.getValue().getPrecoExp().getPequeno();
            }else if(quantidade.get(entry.getKey()) <= 5){
                transportadoras += entry.getValue().getPrecoExp().getMedio();
            }else if(quantidade.get(entry.getKey()) > 5){
                transportadoras += entry.getValue().getPrecoExp().getGrande();
            }
        }

        for (Map.Entry<String, Transportadoras> entry : adicionadasp.entrySet()){
            if(quantidadep.get(entry.getKey()) <= 1){
                transportadoras += entry.getValue().getPrecoPremium().getPequeno();
            }else if(quantidadep.get(entry.getKey()) <= 5){
                transportadoras += entry.getValue().getPrecoPremium().getMedio();
            }else if(quantidadep.get(entry.getKey()) > 5){
                transportadoras += entry.getValue().getPrecoPremium().getGrande();
            }
        }

        double preco = 0.0;
        for (Map.Entry<String, Artigo> entry : current.getArtigos().entrySet()) {
            if(entry.getValue().getPreco() != -1.0){
                preco += entry.getValue().getPreco();
            }
        }
        System.out.println("Artigos: " + current.getArtigos() + "\n"
                + "Preço dos artigos: " + preco + "\n"
                + "Preço das transportadoras: " + transportadoras  + "\n"
                + "Preço total: " + (preco + transportadoras)
        );
    }

    // limpar a encomenda
    private void encomenda_clean(Encomendas current){
        System.out.println("Tem a certeza que quer limpar o carrinho [y/n]?");
        String clean = scin.nextLine();

        if(clean.contains("y")){
            current.clean();
            System.out.println("Limpado com successo!");
        }else{
            System.out.println("Cancelado com successo!");
        }
    }

    // ver receita do vendedor
    private void user_receita(Utilizador logged){
        System.out.println("Receita até ao momento: " + (logged.getRevenue() - (logged.getRevenue()*this.getModel().getVintagecut())));
    }

    // ver produtos que foram comprados
    private void user_bought(Utilizador logged){
        boolean s = true;
        for (Map.Entry<String, Encomendas> entry : logged.getEncomendas().entrySet()) {
            if(!(entry.getValue().getEstado() == 0)){
                s = false;
                System.out.println(entry.getValue().getArtigos());
            }
        }
        if(s){
            System.out.println("Ainda não comprou nenhum artigo!");
        }
    }

    // ver produtos vendidos
    private void user_sold(Utilizador logged){
        for (Map.Entry<String, Artigo> entry : logged.getArtigos().entrySet()) {
            if(entry.getValue().getSold() > 0){
                System.out.println("Vendeu: " + entry.getValue().getSold() + entry.getValue());
            }
        }
        user_receita(logged);
    }

    // ver produtos que tao publicos
    private void user_selling(Utilizador logged){
        for (Map.Entry<String, Artigo> entry : logged.getArtigos().entrySet()) {
            if(entry.getValue().isPublicado()){
                System.out.println(entry.getValue());
            }
        }
    }

    // menu de registo
    private void registar(){
        NewMenu registarMenu = new NewMenu(new String[]{
                "User", "Transportadora", "Coleção"
        });

        registarMenu.setHandler(1, this::registar_user);
        registarMenu.setHandler(2, this::registar_transportadora);
        registarMenu.setHandler(3, this::registar_colecao);
        registarMenu.setTitle("Registar Menu");
        registarMenu.run();

    }

    // registar uma coleção
    private void registar_colecao(){
        String col, cancel = "";
        System.out.println("Nome da coleção:");
        col = scin.nextLine();

        while(this.getModel().getColecao().containsKey(col)){
            System.out.println("Já existe coleção com esse nome!");
            System.out.println("Cancelar a adição [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Nome da coleção:");
            col = scin.nextLine();
        }
        if(cancel.contains("y")) return;

        Colecao cole = new Colecao(col, this.getModel().now());
        cole.setData(this.getModel().now());

        this.getModel().addColecao(cole);
    }

    // registar usuario
    private void registar_user(){
        String email= "", nome, morada, nif="", cancel = "";
        System.out.println("Introduza o novo email:");
        email = scin.nextLine();
        while(!(email.contains("@")) || this.getModel().existsEmail(email)){
            if(!(email.contains("@"))){
                System.out.println("E-mail Inválido! (não contém @)");
            }if(this.getModel().existsEmail(email)){
                System.out.println("Este e-mail já esta registado!");
            }
            System.out.println("Cancelar o registo [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("E-mail: ");
            email = scin.nextLine();
        }
        if(cancel.contains("y")) return;
        System.out.println("Nome: ");
        nome = scin.nextLine();
        System.out.println("Morada: ");
        morada = scin.nextLine();

        System.out.println("Introduza o nif:");
        nif = scin.nextLine();
        while(nif.length() != 9){
            System.out.println("Nif Inválido!");
            System.out.println("Cancelar o registo [y/n]?");
            cancel = scin.nextLine();
            if(cancel.contains("y")) break;
            System.out.println("Nif (123456789): ");
            nif = scin.nextLine();
        }
        if(cancel.contains("y")) return;

        Utilizador user = new Utilizador(email, nome, morada, nif);
        model.RegistarUser(user);

        System.out.println("Id de login: " + user.getID());
    }

    // registar transportadora
    private void registar_transportadora(){
        String imposto, p, m, g, premium, formula, diasatraso;
        Transportadoras transportadora;

        System.out.println("Preço para encomendas pequenas (1 artigo): ");
        p = scin.nextLine();
        System.out.println("Preço para encomendas médias (2 a 5 artigos): ");
        m = scin.nextLine();
        System.out.println("Preço para encomendas grandes (mais que 5 artigos): ");
        g = scin.nextLine();
        System.out.println("Imposto: ");
        imposto = scin.nextLine();

        transportadora = new Transportadoras(Double.parseDouble(p),Double.parseDouble(m) ,Double.parseDouble(g),
                Double.parseDouble(imposto));

        System.out.println("Formula de Cálculo\nkeys:\nvalor - valor base dos 3 tamanhos\nimposto - imposto");
        formula = scin.nextLine();
        transportadora.setFormula(formula);
        transportadora.formula(formula);

        System.out.println("Ativar Premium [y/n]? ");
        premium = scin.nextLine();
        if(premium.equals("y")) {
            transportadora.ativaPremium();
            System.out.println("Formula de Cálculo Premium\nkeys:\nvalor - valor base dos 3 tamanhos\nimposto - imposto");
            formula = scin.nextLine();
            transportadora.setFpremium(formula);
            transportadora.formulaPremium(formula);
        }

        while(true){
            System.out.println("Dias de atraso de envio em relação à compra: ");
            diasatraso = scin.nextLine();
            try{
                transportadora.setDiasAtraso(Integer.parseInt(diasatraso));
                break;
            } catch (NumberFormatException e){
                return;
            }
        }


        System.out.println("Numero de login: " + this.getModel().getTransportadora().size());

        model.addTransportadora(transportadora);
    }

    private sys getModel() {
        return model;
    }

    private void setModel(sys model) {
        this.model = model;
    }
}
