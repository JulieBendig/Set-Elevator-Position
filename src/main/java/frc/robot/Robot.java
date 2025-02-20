// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.*;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.NeutralOut;
import com.ctre.phoenix6.controls.PositionTorqueCurrentFOC;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.hardware.TalonFX;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import frc.robot.sim.PhysicsSim;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private final TalonFX masterMotor = new TalonFX(31);
  private final TalonFX followerMotor = new TalonFX(32);

  /* Be able to switch which control request to use based on a button press */
  /* Start at position 0, use slot 0 */
  private final PositionVoltage m_positionVoltage = new PositionVoltage(0).withSlot(0);
  /* Start at position 0, use slot 1 */
  private final PositionTorqueCurrentFOC m_positionTorque = new PositionTorqueCurrentFOC(0).withSlot(1);
  /* Keep a brake request so we can disable the motor */
  private final NeutralOut m_brake = new NeutralOut();

  private final XboxController m_joystick = new XboxController(0);

  private final Mechanisms m_mechanism = new Mechanisms();

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  public Robot() {
    TalonFXConfiguration masterConfigs = new TalonFXConfiguration();
    TalonFXConfiguration followerConfigs = new TalonFXConfiguration();
    masterConfigs.Slot0.kP = 2.4; // An error of 1 rotation results in 2.4 V output
    masterConfigs.Slot0.kI = 0; // No output for integrated error
    masterConfigs.Slot0.kD = 0.1; // A velocity of 1 rps results in 0.1 V output
    // Peak output of 8 V
    masterConfigs.Voltage.withPeakForwardVoltage(Volts.of(8))
      .withPeakReverseVoltage(Volts.of(-8));

    masterConfigs.Slot1.kP = 60; // An error of 1 rotation results in 60 A output
    masterConfigs.Slot1.kI = 0; // No output for integrated error
    masterConfigs.Slot1.kD = 6; // A velocity of 1 rps results in 6 A output
    // Peak output of 120 A
    masterConfigs.TorqueCurrent.withPeakForwardTorqueCurrent(Amps.of(120))
      .withPeakReverseTorqueCurrent(Amps.of(-120));

    /* Retry config apply up to 5 times, report if failure */
    StatusCode status = StatusCode.StatusCodeNotInitialized;
    for (int i = 0; i < 5; ++i) {
      status = masterMotor.getConfigurator().apply(masterConfigs);
      if (status.isOK()) break;
    }
    if (!status.isOK()) {
      System.out.println("Could not apply masterConfigs, error code: " + status.toString());
    }

    followerMotor.getConfigurator().apply(followerConfigs);
    followerMotor.setControl(new Follower(31, false));

    /* Make sure we start at 0 */
    masterMotor.setPosition(0);
  }

  @Override
  public void robotPeriodic() {
    m_mechanism.update(masterMotor.getPosition());
  }

  @Override
  public void autonomousInit() {}

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void teleopInit() {}

  @Override
  public void teleopPeriodic() {
    double desiredRotations = m_joystick.getLeftY() * 10; // Go for plus/minus 10 rotations
    if (Math.abs(desiredRotations) <= 0.1) { // Joystick deadzone
      desiredRotations = 0;
    }

    if (m_joystick.getLeftBumperButton()) {
      /* Use position voltage */
      masterMotor.setControl(m_positionVoltage.withPosition(desiredRotations));
    } else if (m_joystick.getRightBumperButton()) {
      /* Use position torque */
      masterMotor.setControl(m_positionTorque.withPosition(desiredRotations));
    } else {
      /* Disable the motor instead */
      masterMotor.setControl(m_brake);
    }
  }

  @Override
  public void disabledInit() {}

  @Override
  public void disabledPeriodic() {}

  @Override
  public void testInit() {}

  @Override
  public void testPeriodic() {}

  @Override
  public void simulationInit() {
    PhysicsSim.getInstance().addTalonFX(masterMotor, 0.001);
  }

  @Override
  public void simulationPeriodic() {
    PhysicsSim.getInstance().run();
  }
}
