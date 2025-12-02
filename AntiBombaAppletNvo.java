import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class AntiBombaAppletNvo extends Applet implements ActionListener, Runnable {

    // UI
    private TextField inputField;
    private Button verifyButton;
    private Button resetButton;
    private Label infoLabel;

    // Lógica del juego
    private Stack<String> secuencia;      // pila con códigos (tope = próximo código)
    private Queue<String> errores;        // cola con entradas incorrectas
    private final int MAX_ERRORES = 3;

    // Temporizador
    private int tiempoRestante = 60;  // 60 segundos ***
    private final int TIEMPO_TOTAL = 60;
    private boolean contando = false;

    // Hilo para actualizar temporizador/UI
    private Thread hilo;
    private volatile boolean running = false;

    // Imagen de bomba (GIF animado posible)
    private Image bombaImg;

    @Override
    public void init() {
        setLayout(new BorderLayout());

        // Panel superior con controles
        Panel norte = new Panel();
        norte.add(new Label("Código (00-99):"));
        inputField = new TextField(4);
        norte.add(inputField);

        verifyButton = new Button("Verificar");
        verifyButton.addActionListener(this);
        norte.add(verifyButton);

        resetButton = new Button("Reiniciar");
        resetButton.addActionListener(this);
        norte.add(resetButton);

        infoLabel = new Label("Estado: esperando...", Label.CENTER);
        add(infoLabel, BorderLayout.SOUTH);

        add(norte, BorderLayout.NORTH);

        // Cargar imagen (archivo bomba.gif en la misma carpeta)
        bombaImg = getImage(getCodeBase(), "bomba.gif");

        // Inicializar juego
        reiniciarJuego();
    }

    private void reiniciarJuego() {
        // Generar 4 códigos aleatorios 00..99 y poner en pila
        secuencia = new Stack<>();
        Random rnd = new Random();
        for (int i = 0; i < 4; i++) {
            int num = rnd.nextInt(100); // 0..99
            secuencia.push(String.format("%02d", num));
        }

        errores = new LinkedList<>();
        tiempoRestante = TIEMPO_TOTAL;
        contando = true;
        
        // Descomentar las siguiente línea para mostrar el código requerido en el infoLabel para fines de práctica.
        if (!secuencia.isEmpty()) {
             infoLabel.setText("CÓDIGO REQUERIDO: " + secuencia.peek() + " | Códigos restantes: " + secuencia.size());
         } else {
             infoLabel.setText("Juego listo. Presiona Reiniciar.");
            }
        // ------------------------------------------------------------------

        // Mensaje estándar para el jugador. esta linea se usa en la versión final. <-------
        //infoLabel.setText("Introduce el código. Códigos restantes: " + secuencia.size());


        // Asegurar que se vuelve a cargar la imagen de la bomba NO EXPLOTADA al reiniciar
        bombaImg = getImage(getCodeBase(), "bomba.gif"); 

        // Iniciar hilo si no está corriendo
        if (hilo == null || !hilo.isAlive()) {
            running = true;
            hilo = new Thread(this);
            hilo.start();
        }
        repaint();
    }

    @Override
    public void start() {
        // start puede ser invocado por el entorno del applet; aseguramos que el hilo corra
        if (hilo == null || !hilo.isAlive()) {
            running = true;
            hilo = new Thread(this);
            hilo.start();
        }
    }

    @Override
    public void stop() {
        // detener hilo de forma segura
        running = false;
        contando = false;
        if (hilo != null) {
            hilo.interrupt();
            hilo = null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == verifyButton) {
            verificarCodigo();
        } else if (e.getSource() == resetButton) {
            reiniciarJuego();
        }
    }

    private void verificarCodigo() {
        if (!contando && tiempoRestante <= 0) {
            infoLabel.setText("Juego finalizado (tiempo). Reinicia para jugar otra vez.");
            return;
        }

        if (secuencia == null || secuencia.isEmpty()) {
            infoLabel.setText("¡BOMBA DESARMADA! Reinicia para jugar de nuevo.");
            return;
        }

        String entrada = inputField.getText().trim();
        if (entrada.length() == 0) {
            infoLabel.setText("Introduce un código (00-99).");
            return;
        }

        // Normalizar: permitir '5' como '05' opcionalmente; aquí exigimos 1-2 dígitos
        try {
            int val = Integer.parseInt(entrada);
            if (val < 0 || val > 99) {
                infoLabel.setText("Código fuera de rango (0-99).");
                return;
            }
            String code = String.format("%02d", val);

            // Comparar con el tope de la pila
            String esperado = secuencia.peek();
            if (code.equals(esperado)) {
                secuencia.pop();
                
                if (secuencia.isEmpty()) {
                    contando = false;
                    infoLabel.setText("¡BOMBA DESARMADA!");
                } else {
                    infoLabel.setText("CORRECTO. Códigos restantes: " + secuencia.size());
                    
                    // --- CÓDIGO DE TRAMPA/DEMOSTRACIÓN (COMENTADO PARA JUGADORES) ---
                    // Descomenta la línea de abajo para mostrar el próximo código.
                    infoLabel.setText("CORRECTO. CÓDIGO REQUERIDO: " + secuencia.peek() + " | Códigos restantes: " + secuencia.size());
                }
            } else {
                errores.add(code);
                
                // Mensaje estándar de error
                infoLabel.setText("INCORRECTO. Errores: " + errores.size() + " / " + MAX_ERRORES + ". Códigos restantes: " + secuencia.size());

                // --- CÓDIGO DE TRAMPA/DEMOSTRACIÓN (COMENTADO PARA JUGADORES) ---
                // Descomenta la línea de abajo para mostrar el código requerido con el mensaje de error.
                String nextCode = secuencia.isEmpty() ? "" : " | CÓDIGO REQUERIDO: " + secuencia.peek();
                infoLabel.setText("INCORRECTO. Errores: " + errores.size() + " / " + MAX_ERRORES + nextCode);
                
                if (errores.size() >= MAX_ERRORES) {
                    contando = false;
                    infoLabel.setText("Demasiados errores. Bomba explotó.");
                    
                    // Carga de imagen de explosión al perder por errores
                    bombaImg = getImage(getCodeBase(), "bomb GIF.gif"); 
                }
            }
        } catch (NumberFormatException nfe) {
            infoLabel.setText("Entrada inválida. Solo números 0-99.");
        } finally {
            inputField.setText("");
            repaint();
        }
    }

    // Hilo que disminuye el tiempo y repinta cada segundo
    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                // si se interrumpe salimos si running == false
                if (!running) break;
            }

            if (contando && tiempoRestante > 0) {
                tiempoRestante--;
                if (tiempoRestante == 0) {
                    contando = false;
                    if (!secuencia.isEmpty() && errores.size() < MAX_ERRORES) {
                        infoLabel.setText("¡Tiempo agotado! Bomba explotó.");
                        
                        // Carga de imagen de explosión al perder por tiempo
                        bombaImg = getImage(getCodeBase(), "bomb GIF.gif"); 
                    }
                }
            }

            // si el juego terminó por otras causas, solo repintar para mostrar estado final
            repaint();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        int w = getWidth();
        int h = getHeight();

        // Dibujar imagen de la bomba a la izquierda (si existe)
        if (bombaImg != null) {
            // Mantener relación aspecto, dibujar con tamaño fijo 140x140
            g.drawImage(bombaImg, 20, 60, 140, 140, this);
        } else {
            // si no hay imagen, dibujar una silueta simple
            g.setColor(Color.DARK_GRAY);
            g.fillOval(20, 60, 140, 140);
            g.setColor(Color.BLACK);
            g.drawString("bomb GIF.gif", 30, 140);
        }

        // Dibujar temporizador gráfico (círculo con arco) a la derecha
        int cx = 220;
        int cy = 120;
        int r = 70;

        // fondo
        g.setColor(Color.LIGHT_GRAY);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);

        // arco que representa tiempo restante (verde -> amarillo -> rojo)
        double pct = Math.max(0, tiempoRestante) / (double)TIEMPO_TOTAL; // 0..1, usando TIEMPO_TOTAL (60)
        int ang = (int) Math.round(pct * 360);

        // color según porcentaje
        if (pct > 0.5) g.setColor(new Color(0, 150, 0));        // verde
        else if (pct > 0.2) g.setColor(new Color(200, 150, 0)); // amarillo oscuro
        else g.setColor(new Color(180, 30, 30));                // rojo

        // fillArc: EMPIEZA EN 90 GRADOS, NEGATIVO PARA IR EN SENTIDO HORARIO
        g.fillArc(cx - r, cy - r, r * 2, r * 2, 90, -ang);

        // borde
        g.setColor(Color.BLACK);
        g.drawOval(cx - r, cy - r, r * 2, r * 2);

        // mostrar tiempo en número grande
        g.setColor(Color.BLACK);
        Font font = new Font("Arial", Font.BOLD, 24);
        g.setFont(font);
        String tStr = String.format("%02d", Math.max(0, tiempoRestante));
        int tw = g.getFontMetrics().stringWidth(tStr);
        g.drawString(tStr, cx - tw / 2, cy + 8);

        // Mostrar estado extendido en el centro / parte baja
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        int textoX = 20;
        int textoY = 230;
        g.setColor(Color.BLACK);
        g.drawString("Códigos restantes (pila): " + secuencia.size(), textoX, textoY);
        g.drawString("Errores (cola): " + errores.size() + " / " + MAX_ERRORES, textoX, textoY + 18);

        // Resultado final grande
        if (!contando) {
            String msg = null;
            if (secuencia.isEmpty()) {
                msg = "¡BOMBA DESARMADA!";
            } else if (errores.size() >= MAX_ERRORES) {
                msg = "BOMBA EXPLOTÓ (errores)";
            } else if (tiempoRestante <= 0) {
                msg = "BOMBA EXPLOTÓ (tiempo)";
            }

            if (msg != null) {
                g.setFont(new Font("Arial", Font.BOLD, 20));
                int mw = g.getFontMetrics().stringWidth(msg);
                g.setColor(Color.RED);
                g.drawString(msg, (w / 2) - (mw / 2), h - 30);
            }
        }
    }
}