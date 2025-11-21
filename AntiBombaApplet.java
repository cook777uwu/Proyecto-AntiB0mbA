import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.Stack;
import java.util.Queue;    // Necesario para la Cola
import java.util.LinkedList; // Implementación de la Cola

public class AntiBombaApplet extends Applet implements ActionListener, Runnable {
    
    // --- Variables del Juego (Pila y Cola) ---
    private final int MAX_CODE = 99;
    private final int SEQUENCE_LENGTH = 4; 
    private Stack<Integer> disarmSequence = new Stack<>(); // Pila para secuencia (LIFO)
    
    private final int MAX_ERRORS = 3; // Límite de errores antes de penalizar
    private final int PENALTY_TIME = 10; // Penalización en segundos
    private Queue<String> errorQueue = new LinkedList<>(); // Cola para intentos fallidos (FIFO)
    
    private final int INITIAL_TIME = 60; 
    private int timeLeft;

    // Componentes de la Interfaz Gráfica
    private TextField codeInput = new TextField(3);
    private Button disarmButton = new Button("Ingresar Código");
    private Button resetButton = new Button("Reiniciar");
    private Label timerLabel = new Label("Tiempo: 60s");
    private Label messageLabel = new Label("Introduce el 1er código (de 4).");
    private Label sequenceLabel = new Label("Códigos restantes: 4 | Errores: 0/" + MAX_ERRORS); // Muestra el estado de la Cola

    // Lógica del Cronómetro
    private Thread timerThread = null;
    private boolean gameActive = true;

    // --- INIT (Se mantiene similar) ---
    public void init() {
        setLayout(new BorderLayout());

        // Panel Norte: Mensaje Principal y Secuencia/Errores
        Panel northPanel = new Panel(new GridLayout(2, 1));
        northPanel.add(messageLabel);
        northPanel.add(sequenceLabel);
        add(northPanel, BorderLayout.NORTH);

        // Panel Central: Entrada de Código y Botones
        Panel centerPanel = new Panel(new FlowLayout());
        centerPanel.add(new Label("Código (00-99):"));
        centerPanel.add(codeInput);
        centerPanel.add(disarmButton);
        centerPanel.add(resetButton);
        add(centerPanel, BorderLayout.CENTER);

        // Panel Sur: Cronómetro
        Panel southPanel = new Panel(new FlowLayout());
        southPanel.add(timerLabel);
        add(southPanel, BorderLayout.SOUTH);

        disarmButton.addActionListener(this);
        resetButton.addActionListener(this);
        
        setSize(450, 250);
        resetGame();
    }

    // [start() y stop() se mantienen iguales]
    public void start() {
        if (timerThread == null) {
            timerThread = new Thread(this);
            timerThread.start();
        }
    }

    public void stop() {
        if (timerThread != null) {
            timerThread.interrupt();
            timerThread = null;
        }
    }
    
    // [run() se mantiene igual]
    public void run() {
        Thread currentThread = Thread.currentThread();
        while (currentThread == timerThread && timeLeft > 0) {
            try {
                Thread.sleep(1000); 
                if (!gameActive) continue; 

                timeLeft--;
                timerLabel.setText("Tiempo: " + timeLeft + "s");

                if (timeLeft <= 0) {
                    gameOver(false); 
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    // --- Generación de Secuencia ---
    private void generateSequence() {
        Random rand = new Random();
        disarmSequence.clear();
        for (int i = 0; i < SEQUENCE_LENGTH; i++) {
            disarmSequence.push(rand.nextInt(MAX_CODE + 1));
        }
    }

    // --- Lógica de Reinicio (Añadido limpieza de la Cola) ---
    private void resetGame() {
        generateSequence(); 
        errorQueue.clear(); // Limpiar la cola de errores
        
        timeLeft = INITIAL_TIME;
        gameActive = true;
        
        codeInput.setText("");
        codeInput.setEnabled(true);
        disarmButton.setEnabled(true);
        resetButton.setEnabled(true);

        messageLabel.setText("¡La bomba requiere 4 códigos para ser desarmada!");
        updateStatusLabels(); // Actualizar el estado inicial de la Cola y Pila
        timerLabel.setText("Tiempo: " + timeLeft + "s");

        if (timerThread == null || !timerThread.isAlive()) {
             timerThread = new Thread(this);
             timerThread.start();
        }
    }
    
    // --- NUEVO MÉTODO: Actualiza los Labels de la Pila y la Cola ---
    private void updateStatusLabels() {
        String sequenceInfo;
        if (disarmSequence.empty()) {
            sequenceInfo = "¡Desarmada!";
        } else {
            // Muestra el siguiente código requerido (parte superior de la Pila)
            //sequenceInfo = "Siguiente código: " + formatCode(disarmSequence.peek()); 
        }
        
        String errorInfo = "Errores: " + errorQueue.size() + "/" + MAX_ERRORS;
        
        sequenceLabel.setText("Códigos restantes: " + disarmSequence.size() + " | " + errorInfo);
        //messageLabel.setText(sequenceInfo + " - Ingresa un código.");
        messageLabel.setText("Introduce el siguiente código para desarmar la bomba.");
    }
    
    // --- Lógica de Fin del Juego (Igual) ---
    private void gameOver(boolean win) {
        gameActive = false;
        codeInput.setEnabled(false);
        disarmButton.setEnabled(false);
        
        if (win) {
            messageLabel.setText("¡ÉXITO! Bomba Desarmada. ¡VICTORIA!");
            timerLabel.setText("Tiempo Sobrante: " + timeLeft + "s");
        } else {
            messageLabel.setText("¡BOOM! Tiempo Agotado o Colas llenas.");
            timerLabel.setText("¡DERROTA! Reinicia para intentarlo.");
        }
    }
    
    // [Función de ayuda para formatear el código, igual]
    private String formatCode(int code) {
        return String.format("%02d", code);
    }

    // --- Manejo de Eventos (Botones) ---
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == resetButton) {
            resetGame();
            return;
        }

        if (e.getSource() == disarmButton && gameActive) {
            
            if (disarmSequence.empty()) return;
            
            try {
                int userCode = Integer.parseInt(codeInput.getText().trim());
                
                int requiredCode = disarmSequence.peek(); 
                
                if (userCode >= 0 && userCode <= MAX_CODE) {
                    
                    if (userCode == requiredCode) {
                        // --- LÓGICA DE LA PILA (Éxito) ---
                        disarmSequence.pop(); 
                        codeInput.setText("");
                        
                        if (disarmSequence.empty()) {
                            gameOver(true);
                        }
                        // La actualización de labels se hace después, en el bloque principal
                        
                    } else {
                        // --- LÓGICA DE LA COLA (Fracaso/Penalización) ---
                        
                        // 1. Añadir el intento fallido a la Cola (FIFO)
                        errorQueue.add(formatCode(userCode));
                        
                        // 2. Verificar si la Cola está llena (Penalización)
                        if (errorQueue.size() >= MAX_ERRORS) {
                            // Aplicar Penalización
                            //timeLeft -= PENALTY_TIME;
                            //timerLabel.setText("Tiempo: " + timeLeft + "s (-" + PENALTY_TIME + "s PENALIZACIÓN!)");
                            timeLeft=0; // Penalización inmediata para terminar el juego
                            gameOver(false); // Fin del juego por penalizacións
                            
                            if (timeLeft <= 0) {
                                gameOver(false); // Derrota por tiempo
                            }
                        }
                        
                        messageLabel.setText("Código INCORRECTO. Penalización por error acumulado.");
                    }
                    
                    updateStatusLabels(); // Actualizar el estado de Pila/Cola
                    
                } else {
                    messageLabel.setText("Error: Código fuera del rango 00-99.");
                }
            } catch (NumberFormatException ex) {
                messageLabel.setText("Error: Introduce solo números.");
            }
        }
    }
}