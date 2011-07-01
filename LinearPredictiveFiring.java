package R2R;
import robocode.*;
import java.util.*;
import robocode.util.*;
import java.awt.geom.Point2D;
/**
 * MyClass - a class by (your name here)
 */
public class LinearPredictiveFiring implements IFiringImpl
{
	private double ROBOT_RADIUS = 300;
	
	public void performFiringLogic(AdvancedRobot sourceRobot, TargetRobot targetRobot)
	{
		System.out.print("Firing Logic\n");
		TargetData targetData = targetRobot.getCurrentTargetData();		
		
		// Start with initial guesses at 10 and 20 ticks		
		double impactTime = getImpactTime(sourceRobot, targetData, 10, 20, 0.01); 
		Point2D.Double impactPoint = getEstimatedPosition(targetData, impactTime);
						
		double bulletHeadingDegrees = calcBulletHeading(sourceRobot, impactPoint);
		double turnAngle = Utils.normalRelativeAngleDegrees
			(bulletHeadingDegrees - sourceRobot.getGunHeading());
			
		System.out.print("Turn Angle: " + turnAngle + "\n");
		sourceRobot.setTurnGunRight(turnAngle);
		
		double angleThreshold = calcAngleThreshold(targetRobot.getCurrentTargetData());
		System.out.print("Angle threshold: " + angleThreshold + " Turn Angle: " + turnAngle + "\n");
		if (Math.abs(turnAngle) <= angleThreshold) {
  			// Ensure that the gun is pointing at the correct angle
  			if (
    			(impactPoint.x > 0) &&
 				(impactPoint.x < sourceRobot.getBattleFieldWidth()) &&
 				(impactPoint.y > 0) &&
 				(impactPoint.y < sourceRobot.getBattleFieldHeight())
			) {
    			// Ensure that the predicted impact point is within 
    			// the battlefield
    			sourceRobot.fire(getBulletPower(sourceRobot, targetData));
  			}
		}		
	}

	protected double calcAngleThreshold(TargetData targetData)
	{
		return Math.toDegrees(Math.atan(ROBOT_RADIUS/targetData.getDistance()));
	}

	protected double calcBulletHeading(AdvancedRobot sourceRobot, Point2D.Double impactPoint)
	{
		double dX = (impactPoint.getX() - sourceRobot.getX());
		double dY = (impactPoint.getY() - sourceRobot.getY());

		double distance = Math.sqrt(dX*dX+dY*dY);

		return Math.toDegrees(Math.atan2(dX,dY));												
	}

	protected double getBulletPower(AdvancedRobot sourceRobot, TargetData targetData)
	{
		double distance = targetData.getDistance();
		double power;
		if (distance > 300 || (sourceRobot.getEnergy() < 40 && distance > 100))
		{
			power = 1;
		}
		else if (distance < 50)
		{
			power = 3;
		}
		else
		{
			power = 3 - ((distance-50) * .008);  // Provides a linear increase of power from 1 to 3 as the distance goes from 299 to 51
		}

		System.out.print("Distance: " + distance + " Power: " + power + "\n");
		return power;							
	}

	protected Point2D.Double getEstimatedPosition(TargetData targetData, double time)
	{
		double x = targetData.getLocation().getX() + 
		   	targetData.getVelocity() * time * Math.sin(Math.toRadians(targetData.getHeading()));
		double y = targetData.getLocation().getY() + 
   			targetData.getVelocity() * time * Math.cos(Math.toRadians(targetData.getHeading()));
		return new Point2D.Double(x,y);	
	}

	protected double getImpactTime(AdvancedRobot sourceRobot, TargetData targetData, double t0, double t1, double accuracy) {

		double X = t1;
		double lastX = t0;
		int iterationCount = 0;
		double lastfX = calcBulletAccuracy(sourceRobot, targetData, lastX);

		while ((Math.abs(X - lastX) >= accuracy) && (iterationCount < 15)) {

			iterationCount++;
			double fX = calcBulletAccuracy(sourceRobot, targetData, X);

			if ((fX-lastfX) == 0.0) break;

			double nextX = X - fX*(X-lastX)/(fX-lastfX);
			lastX = X;
			X = nextX;
			lastfX = fX;
		}

		return X;
	}

	private double calcBulletAccuracy(AdvancedRobot sourceRobot, TargetData targetData, double time) 
	{
		// For a given time calculate the expected position of the target
		// and determine if the bullet will be able to get there in time
		// to hit the target.  The different in distance will be returned.
		// The ideal will be a difference in distance of zero.
		double bulletVelocity = 20-3* getBulletPower(sourceRobot, targetData);

		Point2D targetPosition = getEstimatedPosition(targetData, time);
		double dX = (targetPosition.getX() - sourceRobot.getX());
		double dY = (targetPosition.getY() - sourceRobot.getY());

		return Math.sqrt(dX*dX + dY*dY) - bulletVelocity * time;
	}						
														
}
