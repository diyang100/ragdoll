import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

public class LowerLegSprite extends Sprite {
    public double x, y, w, h; // x and y position
    public Image image;

     // Creates a rectangle based at the origin with the specified width and height
    public LowerLegSprite(Image image) {
        super();
        this.initialize(image);
    }

     // Creates a rectangle based at the origin with the specified parent, width, and height
    public LowerLegSprite(Sprite parent, Image image) {
        super(parent);
        this.initialize(image);
    }

    // Initialize the rect at the origin
    private void initialize(Image image) {
        this.image = image;
        x = 0;
        y = 0;
        w = image.getWidth();
        h = image.getHeight();
        origin_x = 0;
        origin_y = 0;
        local_origin_x = 0;
        local_origin_y = 0;

    }
    // Draw on the supplied canvas
    protected void draw(GraphicsContext gc) {
        // save the current graphics context so that we can restore later
        Affine oldMatrix = gc.getTransform();

        // make sure we have the correct transformations for this shape
        gc.setTransform(getFullMatrix());
        gc.drawImage(image, x, y);
        gc.setStroke(Color.BLUE);
        gc.strokeText(localID, x + w/2 - 3, y + h/2 + 3);

        // draw children
        for (Sprite child : children) {
            child.draw(gc);
        }

        // set back to original value since we're done with this branch of the scene graph
        gc.setTransform(oldMatrix);
    }

    // Check if the point is contained by this shape
    // This cannot be abstract, since it relies on knowledge of the
    // specific type of shape for the hit test.
    @Override
    protected boolean contains(Point2D p) {
        try {
            // Use inverted matrix to move the mouse click so that it's
            // relative to the shape model at the origin.
            Point2D pointAtOrigin = getFullMatrix().createInverse().transform(p);

            // Perform the hit test relative to the shape model's
            // untranslated coordinates at the origin
            return new Rectangle(x, y, w, h).contains(pointAtOrigin);

        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    void rotate(double theta) throws NonInvertibleTransformException {
        Affine fullMatrix = getFullMatrix();
        Affine inverse = fullMatrix.createInverse();

        // move to the origin, rotate and move back
        matrix.prepend(inverse);
        matrix.prependRotation(theta, local_origin_x, local_origin_y);
        matrix.prepend(fullMatrix);

        relativeRotation.prependRotation(theta);

        Affine rotate = new Affine();
        rotate.prependRotation(theta, local_origin_x, local_origin_y);

        for (Sprite child : children) {
            Affine childFullMatrix = child.getFullMatrix();
            child.matrix.prepend(childFullMatrix.createInverse());
            child.matrix.prepend(child.relativeRotation);
            child.matrix.prepend(child.relativeTranslation);
            child.matrix.prepend(rotate);
            child.matrix.prepend(fullMatrix);
        }
    }

}

