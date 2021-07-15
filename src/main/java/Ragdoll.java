import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.util.HashMap;

public class Ragdoll extends Application {
	final float screen_width = 1280;
	final float screen_height = 1024;
	double previous_x, previous_y;
	Point2D clickedPos = new Point2D(0, 0);
	Sprite selectedSprite;
	HashMap<Sprite, OPERATION> sprite_Operation = new HashMap<>();

	enum OPERATION {TRANSLATE, ROTATE_AND_SCALE, ROTATE}
	OPERATION operation = OPERATION.TRANSLATE;

	@Override
	public void start(Stage stage) {
		stage.setResizable(false);
		stage.setTitle("Ragdoll");
		stage.setOnCloseRequest(event -> System.exit(0));

		// setup a canvas to use as a drawing surface
		Canvas canvas = new Canvas(screen_width, screen_height);
		Label mouseCoords = new Label(Double.toString(previous_x) + ", " + Double.toString(previous_y));
		Scene scene = new Scene(new VBox(new HBox(mouseCoords), canvas), screen_width, screen_height); // TODO: add menu here


		// create hierarchy of sprites
		Sprite root = createSprites();

		// add listeners
		// click selects the shape under the cursor
		// we have sprites do it since they track their own locations
		canvas.setOnMousePressed(mouseEvent -> {
			mouseCoords.setText(Double.toString(previous_x) + ", " + Double.toString(previous_y));
			Sprite hit = root.getSpriteHit(mouseEvent.getX(), mouseEvent.getY());
			if (hit != null) {
				selectedSprite = hit;
				operation = sprite_Operation.get(selectedSprite);
				if (operation == null) {
					System.out.println("operation failed: no transformation mapping for selected sprite");
					operation = OPERATION.TRANSLATE;
				}
				System.out.println("Selected " + selectedSprite.toString());
				previous_x = mouseEvent.getX();
				previous_y = mouseEvent.getY();
				clickedPos = new Point2D(previous_x, previous_y);
			}
		});

		// un-selects any selected shape
		canvas.setOnMouseReleased( mouseEvent -> {
			selectedSprite = null;
			System.out.println("Unselected");
		});

		// dragged translates the shape based on change in mouse position
		// since shapes are defined relative to one another, they will follow their parent
		canvas.setOnMouseDragged(mouseEvent -> {
			mouseCoords.setText(Double.toString(previous_x) + ", " + Double.toString(previous_y));
			if (selectedSprite != null) {
				switch(operation) {
					case TRANSLATE:
						// translate shape to follow the mouse cursor
						double dx = mouseEvent.getX() - previous_x;
						double dy = mouseEvent.getY() - previous_y;
						try {
							selectedSprite.translate(dx, dy);
						} catch (NonInvertibleTransformException e) {
							e.printStackTrace();
						}
						System.out.println(".. moved "
								+ selectedSprite.toString()
								+ " from (" + previous_x + "," + previous_y + ")"
								+ " to (" + mouseEvent.getX() + "," + mouseEvent.getY() + ")"
								+ " -- dx: " + dx + ", dy: " + dy);
						break;
					case ROTATE_AND_SCALE:
						//Check rotate
						double distance1 = Math.sqrt(Math.pow(mouseEvent.getX() - previous_x, 2) + Math.pow(mouseEvent.getY() - previous_y, 2));
						double theta1 = Math.atan(distance1);
						if (mouseEvent.getX() > selectedSprite.origin_x && mouseEvent.getY() > selectedSprite.origin_y){
							if (mouseEvent.getX() > previous_x && mouseEvent.getY() < previous_y) {
								theta1 *= -1;
							} else if (mouseEvent.getX() < previous_x && mouseEvent.getY() > previous_y) {
								theta1 *= 1;
							} else {
								theta1 = 0;
							}
						} else if (mouseEvent.getX() > selectedSprite.origin_x && mouseEvent.getY() < selectedSprite.origin_y) {
							if (mouseEvent.getX() < previous_x && mouseEvent.getY() < previous_y) {
								theta1 *= -1;
							} else if (mouseEvent.getX() > previous_x && mouseEvent.getY() > previous_y) {
								theta1 *= 1;
							} else {
								theta1 = 0;
							}
						} else if (mouseEvent.getX() < selectedSprite.origin_x && mouseEvent.getY() < selectedSprite.origin_y) {
							if (mouseEvent.getX() < previous_x && mouseEvent.getY() > previous_y) {
								theta1 *= -1;
							} else if (mouseEvent.getX() > previous_x && mouseEvent.getY() < previous_y) {
								theta1 *= 1;
							} else {
								theta1 = 0;
							}
						} else if (mouseEvent.getX() < selectedSprite.origin_x && mouseEvent.getY() > selectedSprite.origin_y) {
							if (mouseEvent.getX() > previous_x && mouseEvent.getY() > previous_y) {
								theta1 *= -1;
							} else if (mouseEvent.getX() < previous_x && mouseEvent.getY() < previous_y) {
								theta1 *= 1;
							} else {
								theta1 = 0;
							}
						}
						try {
							selectedSprite.rotate(theta1);
						} catch (NonInvertibleTransformException e) {
							e.printStackTrace();
						}

						// Check scale
						double prevDistance = Math.sqrt(Math.pow(previous_x - selectedSprite.origin_x, 2) + Math.pow(previous_y - selectedSprite.origin_y, 2));
						double currDistance = Math.sqrt(Math.pow(mouseEvent.getX() - selectedSprite.origin_x, 2) + Math.pow(mouseEvent.getY() - selectedSprite.origin_y, 2));
						if (currDistance > prevDistance) {
							try {
								selectedSprite.scale(1, 1.01);
							} catch (NonInvertibleTransformException e) {
								e.printStackTrace();
							}
						} else {
							try {
								selectedSprite.scale(1, 0.99);
							} catch (NonInvertibleTransformException e) {
								e.printStackTrace();
							}
						}
						break;
					case ROTATE:
						double distance = Math.sqrt(Math.pow(mouseEvent.getX() - previous_x, 2) + Math.pow(mouseEvent.getY() - previous_y, 2));
						double theta = Math.atan(distance);
						if (mouseEvent.getX() > selectedSprite.origin_x && mouseEvent.getY() > selectedSprite.origin_y){
							if (mouseEvent.getX() > previous_x && mouseEvent.getY() < previous_y) {
								theta *= -1;
							} else if (mouseEvent.getX() < previous_x && mouseEvent.getY() > previous_y) {
								theta *= 1;
							} else {
								theta = 0;
							}
						} else if (mouseEvent.getX() > selectedSprite.origin_x && mouseEvent.getY() < selectedSprite.origin_y) {
							if (mouseEvent.getX() < previous_x && mouseEvent.getY() < previous_y) {
								theta *= -1;
							} else if (mouseEvent.getX() > previous_x && mouseEvent.getY() > previous_y) {
								theta *= 1;
							} else {
								theta = 0;
							}
						} else if (mouseEvent.getX() < selectedSprite.origin_x && mouseEvent.getY() < selectedSprite.origin_y) {
							if (mouseEvent.getX() < previous_x && mouseEvent.getY() > previous_y) {
								theta *= -1;
							} else if (mouseEvent.getX() > previous_x && mouseEvent.getY() < previous_y) {
								theta *= 1;
							} else {
								theta = 0;
							}
						} else if (mouseEvent.getX() < selectedSprite.origin_x && mouseEvent.getY() > selectedSprite.origin_y) {
							if (mouseEvent.getX() > previous_x && mouseEvent.getY() > previous_y) {
								theta *= -1;
							} else if (mouseEvent.getX() < previous_x && mouseEvent.getY() < previous_y) {
								theta *= 1;
							} else {
								theta = 0;
							}
						}
						try {
							selectedSprite.rotate(theta);
						} catch (NonInvertibleTransformException e) {
							e.printStackTrace();
						}
						break;
				}

				// draw tree in new position
				draw(canvas, root);

				// save coordinates for next event
				previous_x = mouseEvent.getX();
				previous_y = mouseEvent.getY();
			}
		});

		// draw the sprites on the canvas
		draw(canvas, root);

		// show the scene including the canvas
		stage.setScene(scene);
		stage.show();
	}


	private void draw(Canvas canvas, Sprite root) {
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		root.draw(gc);
	}
	
	private Sprite createSprites() {
		// create a bunch of different sprites at the origin
		TorsoSprite torso = new TorsoSprite(new Image("body.png"));
		HeadSprite head = new HeadSprite(new Image("head.png"));
		UpperArmSprite leftUpArm = new UpperArmSprite(new Image("left_upper_arm.png"));
		UpperArmSprite rightUpArm = new UpperArmSprite(new Image("right_upper_arm.png"));
		LowerArmSprite leftLowArm = new LowerArmSprite(new Image("left_lower_arm.png"));
		LowerArmSprite rightLowArm = new LowerArmSprite(new Image("right_lower_arm.png"));
		HandSprite leftHand = new HandSprite(new Image("left_hand.png"));
		HandSprite rightHand = new HandSprite(new Image("right_hand.png"));
		UpperLegSprite leftUpLeg = new UpperLegSprite(new Image("left_upper_leg.png"));
		UpperLegSprite rightUpLeg = new UpperLegSprite(new Image("right_upper_leg.png"));
		LowerLegSprite leftLowLeg = new LowerLegSprite(new Image("left_lower_leg.png"));
		LowerLegSprite rightLowLeg = new LowerLegSprite(new Image("right_lower_leg.png"));
		FootSprite leftFoot = new FootSprite(new Image("left_foot.png"));
		FootSprite rightFoot = new FootSprite(new Image("right_foot.png"));

		 // build scene graph aka tree from them
		torso.addChild(head);
		torso.addChild(leftUpArm);
		torso.addChild(rightUpArm);
		torso.addChild(leftUpLeg);
		torso.addChild(rightUpLeg);

		leftUpArm.addChild(leftLowArm);
		rightUpArm.addChild(rightLowArm);

		leftUpLeg.addChild(leftLowLeg);
		rightUpLeg.addChild(rightLowLeg);

		leftLowArm.addChild(leftHand);
		rightLowArm.addChild(rightHand);

		leftLowLeg.addChild(leftFoot);
		rightLowLeg.addChild(rightFoot);

		// translate them to a starting position
		// this also places them beside one another
		try {
			torso.translate(screen_width/2 - torso.w/2, 160);
			head.translate(0, -140);
			leftUpArm.translate(-7, 30);
			rightUpArm.translate(152, 37);

			leftUpLeg.translate(5, torso.h - 40);
			rightUpLeg.translate(torso.w/2-10, torso.h-40);

			leftLowArm.translate(-33, leftUpArm.h-6);
			rightLowArm.translate(18, rightUpArm.h-12);

			leftLowLeg.translate(0, leftUpLeg.h);
			rightLowLeg.translate(28, rightUpLeg.h);

			leftHand.translate(leftLowArm.w - leftHand.w, leftLowArm.h);
			rightHand.translate(10, rightLowArm.h-12);

			leftFoot.translate(-30, leftLowLeg.h-2);
			rightFoot.translate(20, rightLowLeg.h-4);
		} catch (NonInvertibleTransformException e) {
			e.printStackTrace();
		}

		// Add transformation for each body part
		sprite_Operation.put(torso, OPERATION.TRANSLATE);
		sprite_Operation.put(head, OPERATION.ROTATE);
		sprite_Operation.put(leftUpArm, OPERATION.ROTATE);
		sprite_Operation.put(rightUpArm, OPERATION.ROTATE);

		sprite_Operation.put(leftUpLeg, OPERATION.ROTATE_AND_SCALE);
		sprite_Operation.put(rightUpLeg, OPERATION.ROTATE_AND_SCALE);
		sprite_Operation.put(leftLowLeg, OPERATION.ROTATE_AND_SCALE);
		sprite_Operation.put(rightLowLeg, OPERATION.ROTATE_AND_SCALE);

		sprite_Operation.put(leftLowArm, OPERATION.ROTATE);
		sprite_Operation.put(rightLowArm, OPERATION.ROTATE);
		sprite_Operation.put(leftHand, OPERATION.ROTATE);
		sprite_Operation.put(rightHand, OPERATION.ROTATE);

		sprite_Operation.put(leftFoot, OPERATION.ROTATE);
		sprite_Operation.put(rightFoot, OPERATION.ROTATE);

		// return root of the tree
		return torso;
	}
}
