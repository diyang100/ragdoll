import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import java.util.Vector;

/**
 * A building block for creating your own shapes
 * These explicitly support parent-child relationships between nodes
 */

public abstract class Sprite {
    static int spriteID = 0;
    final String localID;

    protected Sprite parent = null;
    public Affine matrix = new Affine();
    protected Vector<Sprite> children = new Vector<Sprite>();
    public double origin_x = 0;
    public double origin_y = 0;
    public double local_origin_x = 0;
    public double local_origin_y = 0;
    public Affine relativeTranslation = new Affine();
    public Affine relativeScaling = new Affine();
    public Affine relativeRotation = new Affine();
    public double rotationLimit = 50;
    public double scaleLimit = 2;
    public double rotationCounter = 1;
    public double scaleCounter = 1;

    public Sprite() {
        localID = String.valueOf(++spriteID);
    }

    public Sprite(Sprite parent) {
        this();
        if (parent != null) {
            parent.addChild(this);
        }
    }

    // maintain hierarchy
    public void addChild(Sprite s) {
        children.add(s);
        s.setParent(this);
    }

    public Sprite getParent() {
        return parent;
    }
    private void setParent(Sprite s) {
        this.parent = s;
    }

    // transformations
    // these will pre-concat to the sprite's affine matrix
    void translate(double dx, double dy) throws NonInvertibleTransformException {
        matrix.prependTranslation(dx, dy);
        relativeTranslation.prependTranslation(dx, dy);
        Affine translation = new Affine();
        translation.prependTranslation(dx, dy);
        fixTranslateChildren(translation, translation.createInverse());

        translateOrigin(dx, dy);
    }

    void translateOrigin(double dx, double dy) {
        origin_x += dx;
        origin_y += dy;
        for (Sprite child : children) {
            child.translateOrigin(dx, dy);
        }
    }

    void fixTranslateChildren(Affine translation, Affine inverse) {
        for (Sprite child : children) {
            child.matrix.append(inverse);
            child.matrix.prepend(translation);
            child.fixTranslateChildren(translation, inverse);
        }
    }

    void rotate(double theta) throws NonInvertibleTransformException {
//        System.out.println(rotationCounter);
        if (rotationCounter > rotationLimit) {
            if (rotationCounter + theta > rotationCounter) {
                return;
            } else {
                rotationCounter += theta;
            }
        } else if (rotationCounter < -rotationLimit) {
            if (rotationCounter + theta < rotationCounter) {
                return;
            } else {
                rotationCounter += theta;
            }
        } else {
            rotationCounter += theta;
        }

        Affine fullMatrix = getFullMatrix();
        Affine inverse = fullMatrix.createInverse();

        // move to the origin, rotate and move back
        matrix.prepend(inverse);
        matrix.prependRotation(theta, local_origin_x, local_origin_y);
        matrix.prepend(fullMatrix);
        relativeRotation.prependRotation(theta, local_origin_x, local_origin_y);

        Affine rotate = new Affine();
        rotate.prependRotation(theta, local_origin_x, local_origin_y);

        for (Sprite child : children) {
            Affine childFullMatrix = child.getFullMatrix();
            child.matrix.prepend(childFullMatrix.createInverse());
            child.matrix.prepend(child.relativeRotation);
            child.matrix.prepend(child.relativeTranslation);
            child.matrix.prepend(rotate);
            child.matrix.prepend(fullMatrix);

            for (Sprite grandChild : child.children) {
                Affine grandChildFullMatrix = grandChild.getFullMatrix();
                grandChild.matrix.prepend(grandChildFullMatrix.createInverse());

                grandChild.matrix.prepend(grandChild.relativeRotation);
                grandChild.matrix.prepend(grandChild.relativeTranslation);
                grandChild.matrix.prepend(child.relativeRotation);
                grandChild.matrix.prepend(child.relativeTranslation);
                grandChild.matrix.prepend(rotate);
                grandChild.matrix.prepend(fullMatrix);
            }
        }
    }

    void scale(double sx, double sy) throws NonInvertibleTransformException {
        System.out.println(scaleCounter);
        if (scaleCounter > scaleLimit && scaleCounter * sy > scaleCounter) {
            sy = 1;
        } else if (scaleCounter < 1/scaleLimit && scaleCounter * sy < scaleCounter) {
            sy = 1;
        } else {
            scaleCounter *= sy;
        }
        Affine fullMatrix = getFullMatrix();
        Affine inverse = fullMatrix.createInverse();

        // move to the origin, scale and move back
        matrix.prepend(inverse);
        matrix.prependScale(sx, sy);
        matrix.prepend(fullMatrix);

        relativeScaling.prependScale(sx, sy);

        Affine scale = new Affine();
        scale.prependScale(sx, sy);

//        scaleOrigin(scale);

        for (Sprite child : children) {
            if (child.children.size() == 0) {
                child.relativeScaling.prepend(scale.createInverse());

                Affine childFullMatrix = child.getFullMatrix();
                child.matrix.prepend(childFullMatrix.createInverse());
                child.matrix.prepend(child.relativeScaling);
                child.matrix.prepend(child.relativeRotation);
                child.matrix.prepend(child.relativeTranslation);
                child.matrix.prepend(fullMatrix);
            } else {
                Affine childFullMatrix = child.getFullMatrix();
                child.matrix.prepend(childFullMatrix.createInverse());
                child.matrix.prepend(child.relativeScaling);
                child.matrix.prepend(child.relativeRotation);
                child.matrix.prepend(child.relativeTranslation);
                child.matrix.prepend(getFullMatrix());
                for (Sprite grandChild : child.children) {
                    grandChild.relativeScaling.prepend(scale.createInverse());

                    Affine grandChildFullMatrix = grandChild.getFullMatrix();
                    grandChild.matrix.prepend(grandChildFullMatrix.createInverse());
                    grandChild.matrix.prepend(grandChild.relativeScaling);
                    grandChild.matrix.prepend(grandChild.relativeRotation);
                    grandChild.matrix.prepend(grandChild.relativeTranslation);
                    grandChild.matrix.prepend(child.relativeScaling);
                    grandChild.matrix.prepend(child.relativeRotation);
                    grandChild.matrix.prepend(child.relativeTranslation);
                    grandChild.matrix.prepend(fullMatrix);
                }
            }
        }
    }

    void scaleOrigin(Affine scale) {
        for (Sprite child : children) {
            Point2D childOrigin = new Point2D(1, 1);
            childOrigin = scale.transform(childOrigin);
            child.origin_x += childOrigin.getX();
            child.origin_y += childOrigin.getY();
            child.scaleOrigin(scale);
        }
    }

    Affine getLocalMatrix() { return matrix; }
    Affine getFullMatrix() {
        Affine fullMatrix = getLocalMatrix().clone();
        if (parent != null) {
            fullMatrix.append(parent.getFullMatrix());
        }
        return fullMatrix;
    }

    // hit tests
    // these cannot be handled in the base class, since the actual hit tests are dependend on the type of shape
    protected abstract boolean contains(Point2D p);
    protected boolean contains(double x, double y) {
        return contains(new Point2D(x, y));
    }

    // we can walk the tree from the base class, since we rely on the specific sprites to check containment
    protected Sprite getSpriteHit(double x, double y) {
        // check me first...
        Sprite hit = null;
        if (this.contains(x, y)) {
            hit = this;
        }

        // if no match above, recurse through children and return the first hit
        for (Sprite sprite : children) {
            if (sprite.getSpriteHit(x, y) != null) {
                hit = sprite.getSpriteHit(x, y);
            }
        }

        return hit;
    }

    // drawing method
    protected abstract void draw(GraphicsContext gc);

    // debugging
    public String toString() { return "Sprite " + localID; }
}
