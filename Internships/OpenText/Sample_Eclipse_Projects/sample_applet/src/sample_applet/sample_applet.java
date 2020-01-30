package sample_applet;
import java.applet.Applet;
import java.awt.Color;
import java.awt.Graphics;
public class sample_applet extends Applet{
	public void init() {}
	public void stop() {}
	public void paint(Graphics a) {
		a.setColor(Color.BLACK);
		a.fillRect(0, 0, 400, 400);
		a.setColor(Color.GREEN);
		a.drawRect(20, 20, 30, 50);
		a.setColor(Color.RED);
		a.fillOval(60, 70, 20, 50);
		a.drawString("Welcome in Java Applet.",40,20);
	}

}
