package br.com.test;

import com.google.common.hash.HashCode;

import java.security.NoSuchAlgorithmException;

import br.com.util.Utils;

/**
 * Created by MarioJ on 20/08/15.
 */
public class UtilsTest {

    public static void main(String[] args) {

        String s1 = "text one";
        String s2 = "text one";

        try {

            HashCode hcS1 = Utils.sha1(s1);
            HashCode hcS2 = Utils.sha1(s2);

            System.out.println(hcS1.equals(hcS2));

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

}
