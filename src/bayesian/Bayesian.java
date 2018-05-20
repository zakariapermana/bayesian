/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bayesian;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author ZAKARIA
 */
public class Bayesian {

    /**
     * @param args the command line arguments
     */
    private final String url = "jdbc:postgresql://localhost:5432/olahraga";
    private final String user = "mdi";
    private final String password = "123";

    /**
     * Connect to the PostgreSQL database
     *
     * @return a Connection object
     */
    public Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
//            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return conn;
    }

    public static void main(String[] args) throws SQLException {
        // TODO code application logic here
        Bayesian bayesian = new Bayesian();
        bayesian.connect();

        Scanner in = new Scanner(System.in);
        System.out.print("Cuaca \t\t: ");
        String cuaca = in.next();
        System.out.print("\n");
        System.out.print("Temperatur \t: ");
        String temperatur = in.next();
        System.out.print("\n");
        System.out.print("Kecepatan Angin : ");
        String angin = in.next();
        System.out.print("\n");

        Statement stmt = null;
        String query = "select distinct olahraga, count(olahraga) jml from data_training group by 1";
        stmt = bayesian.connect().createStatement();
        ResultSet rs = stmt.executeQuery(query);
        float ya = 1;
        float tidak = 1;
        while (rs.next()) {
            if (rs.getString("olahraga").equals("ya")) {
                ya = rs.getFloat("jml");
            } else {
                tidak = rs.getInt("jml");
            }
        }
        float prob_ya = ya / (ya + tidak);
        float prob_tidak = tidak / (ya + tidak);

        query = "select cuaca, coalesce(max(ya),0) ya, coalesce(max(tidak),0) tidak from \n"
                + "(select cuaca, cuaca.jml, case when olrg.olahraga='ya' then cuaca.jml::float/olrg.jml::float end as ya, \n"
                + "case when olrg.olahraga='tidak' then cuaca.jml::float/olrg.jml::float end as tidak\n"
                + "from (select cuaca, olahraga, count(*) jml from data_training group by 1,2) cuaca\n"
                + "left join (select distinct olahraga, count(olahraga) jml from data_training group by 1) olrg on cuaca.olahraga=olrg.olahraga) dt_cuaca\n"
                + "group by 1;";
        stmt = bayesian.connect().createStatement();
        rs = stmt.executeQuery(query);
        float cuaca_ya = 1;
        float cuaca_tidak = 1;
        while (rs.next()) {
            if (rs.getString("cuaca").equals(cuaca)) {
                cuaca_ya = rs.getFloat("ya");
                cuaca_tidak = rs.getFloat("tidak");
            }
        }

        query = "select temperatur, coalesce(max(ya),0) ya, coalesce(max(tidak),0) tidak from \n"
                + "(select temperatur, temperatur.jml, case when olrg.olahraga='ya' then temperatur.jml::float/olrg.jml::float end as ya, \n"
                + "case when olrg.olahraga='tidak' then temperatur.jml::float/olrg.jml::float end as tidak\n"
                + "from (select temperatur, olahraga, count(*) jml from data_training group by 1,2) temperatur\n"
                + "left join (select distinct olahraga, count(olahraga) jml from data_training group by 1) olrg on temperatur.olahraga=olrg.olahraga) dt_temperature\n"
                + "group by 1;";
        stmt = bayesian.connect().createStatement();
        rs = stmt.executeQuery(query);
        float temperatur_ya = 1;
        float temperatur_tidak = 1;
        while (rs.next()) {
            if (rs.getString("temperatur").equals(temperatur)) {
                temperatur_ya = rs.getFloat("ya");
                temperatur_tidak = rs.getFloat("tidak");
            }
        }

        query = "select kecepatan_angin, coalesce(max(ya),0) ya, coalesce(max(tidak),0) tidak from \n"
                + "(select kecepatan_angin, kecepatan_angin.jml, case when olrg.olahraga='ya' then kecepatan_angin.jml::float/olrg.jml::float end as ya, \n"
                + "case when olrg.olahraga='tidak' then kecepatan_angin.jml::float/olrg.jml::float end as tidak\n"
                + "from (select kecepatan_angin, olahraga, count(kecepatan_angin) jml from data_training group by 1,2) kecepatan_angin\n"
                + "left join (select distinct olahraga, count(olahraga) jml from data_training group by 1) olrg on kecepatan_angin.olahraga=olrg.olahraga) dt_kecepatan_angin\n"
                + "group by 1;";
        stmt = bayesian.connect().createStatement();
        rs = stmt.executeQuery(query);
        float angin_ya = 1;
        float angin_tidak = 1;
        while (rs.next()) {
            if (rs.getString("kecepatan_angin").equals(angin)) {
                angin_ya = rs.getFloat("ya");
                angin_tidak = rs.getFloat("tidak");
            }
        }

        float olahraga_ya = cuaca_ya * temperatur_ya * angin_ya * prob_ya;
        float olahraga_tidak = cuaca_tidak * temperatur_tidak * angin_tidak * prob_tidak;

        System.out.println("Prob. Ya\t:" + prob_ya + "\tProb. Tidak\t:" + prob_tidak);
        System.out.println("----------------------------------------------");
        System.out.println("Variable\t\tYa\t\tTidak");
        System.out.println("----------------------------------------------");
        query = "SELECT column_name FROM information_schema.columns WHERE table_name = 'data_training' and column_name!='id';";
        stmt = bayesian.connect().createStatement();
        rs = stmt.executeQuery(query);
        while (rs.next()) {
            if (rs.getString("column_name").equals("cuaca")) {
                System.out.println(rs.getString("column_name") + "\t\t\t" + cuaca_ya + "\t\t" + cuaca_tidak);
            }
            if (rs.getString("column_name").equals("temperatur")) {
                System.out.println(rs.getString("column_name") + "\t\t" + temperatur_ya + "\t\t" + temperatur_tidak);
            }
            if (rs.getString("column_name").equals("kecepatan_angin")) {
                System.out.println(rs.getString("column_name") + "\t\t" + angin_ya + "\t\t" + angin_tidak);
            }
        }
        System.out.println("----------------------------------------------");
        System.out.println("Prob. Olahraga (Ya)\t: " + olahraga_ya);
        System.out.println("Prob. Olahraga (Tidak)\t: " + olahraga_tidak);
        if (olahraga_ya > olahraga_tidak) {
            System.out.println("Apakah berolahraga\t: ya");
        } else {
            System.out.println("Apakah berolahraga\t: tidak");
        }

    }

}
