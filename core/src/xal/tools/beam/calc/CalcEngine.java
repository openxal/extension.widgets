/**
 * CalcEngine.java
 *
 * Author  : Christopher K. Allen
 * Since   : Aug 14, 2013
 */
package xal.tools.beam.calc;

import xal.model.probe.traj.IPhaseState;
import xal.tools.math.r3.R3;
import xal.tools.math.r4.R4;
import xal.tools.math.r4.R4x4;
import xal.tools.math.r6.R6;
import xal.tools.math.r6.R6x6;
import xal.tools.beam.PhaseMap;
import xal.tools.beam.PhaseMatrix;
import xal.tools.beam.PhaseVector;
import xal.tools.beam.Twiss;
import xal.tools.beam.PhaseMatrix.IND;
import xal.tools.data.DataAdaptor;
import Jama.Matrix;

/**
 * Class <code></code>.
 *
 *
 * @author Christopher K. Allen
 * @author Patrick Scruggs
 * @since  Aug 14, 2013
 */
public abstract class CalcEngine implements IPhaseState {



    /*
     * Global Constants
     */


    /** small number used to determine the conditioning of the linear system */
    static final private double    DBL_EPS = 1.0e-6;
    
    /** the number 2&pi; */
    static final private double     DBL_2PI = 2 * Math.PI;
    

    /** This is an artifact of <code>TransferMapState</code> number of (phase plane) modes */
    static final private int NUM_MODES = 3;


    /*
     * Initialization
     */

    public CalcEngine() {
    }

    
    public PhaseVector phaseCoordinates() { return null; }


    
    //	/** 
    //	 * Calculate the fixed point solution vector representing the closed orbit at the location of this element.
    //	 * We find the fixed point for the six phase space coordinates.
    //	 * The equation to solve is <code>Ax + b = 0</code> where <code>A</code> is the 6x6 submatrix less the identity
    //	 * matrix and <code>b</code> is the 7th column excluding the 7th row element.  The reason for this is that the
    //	 * fixed point is defined by the point for which the transfer map maps to the same point.  This is
    //	 * <code>M * v = v</code>.  
    //	 * 
    //	 * @return the fixed point solution
    //	 */
    //	public PhaseVector calculateFixedPoint(PhaseMatrix matPhi) {
    //		Matrix A = _phaseMatrix.getMatrix().getMatrix( 0, PhaseMatrix.IND_ZP, 0, PhaseMatrix.IND_ZP ).minus( Matrix.identity(PhaseMatrix.IND_ZP+1, PhaseMatrix.IND_ZP+1) );
    //		Matrix b = _phaseMatrix.getMatrix().getMatrix( 0, PhaseMatrix.IND_ZP, PhaseMatrix.IND_HOM, PhaseMatrix.IND_HOM ).times( -1 );
    //
    //		//sako
    //		//Matrix MZ = m_matPhase.getMatrix(IND_Z,IND_ZP,IND_Z,IND_ZP);
    //		//      System.out.println("det(MZ), det(A) = "+MZ.det()+" "+A.det());
    //		//      System.out.println("###### MZ = ("+MZ.get(0,0)+","+MZ.get(0,1)+")("+MZ.get(1,0)+","+MZ.get(1,1)+")");
    //
    //		PhaseVector sol;
    //
    //		if (A.det()==0) {
    //			Matrix Axy = _phaseMatrix.getMatrix().getMatrix( 0, PhaseMatrix.IND_YP, 0, PhaseMatrix.IND_YP ).minus( Matrix.identity(PhaseMatrix.IND_YP+1, PhaseMatrix.IND_YP+1) );
    //			Matrix bxy = _phaseMatrix.getMatrix().getMatrix( 0, PhaseMatrix.IND_YP, PhaseMatrix.IND_HOM, PhaseMatrix.IND_HOM ).times( -1 );
    //			Matrix solutionxy = Axy.solve(bxy);
    //			//System.out.println("A.det()=0, sxy solved");
    //			sol = new PhaseVector( solutionxy.get(PhaseMatrix.IND_X, 0), solutionxy.get(PhaseMatrix.IND_XP, 0), solutionxy.get(PhaseMatrix.IND_Y, 0), solutionxy.get(PhaseMatrix.IND_YP, 0), 0, 0 );//sako, check z, zp components!
    //		} else {
    //
    //			Matrix solution = A.solve(b);
    //			sol = new PhaseVector( solution.get(PhaseMatrix.IND_X, 0), solution.get(PhaseMatrix.IND_XP, 0), solution.get(PhaseMatrix.IND_Y, 0), solution.get(PhaseMatrix.IND_YP, 0), solution.get(PhaseMatrix.IND_Z, 0), solution.get(PhaseMatrix.IND_ZP, 0) );
    //		}
    //		return sol;
    //	}

    /**
     * <p>
     * Taken from <code>TransferMapState</code>.
     * </p>
     * <p> 
     * Calculate the fixed point solution vector representing the closed orbit at the 
     * location of this element.
     * We first attempt to find the fixed point for the full six phase space coordinates.
     * Let <b>&Phi;</b> denote the one-turn map for a ring.  The fixed point 
     * <b>z</b> &in; <b>R</b><sup>6</sup>&times;{1} in homogeneous phase space coordinates
     * is that which is invariant under <b>&Phi;</b>, that is,
     * <br/>
     * <br/>
     * &nbsp; &nbsp; <b>&Phi;z</b> = <b>z</b> .
     * <br/>
     * <br/> 
     * This method returns that vector <b>z</b>.  
     * </p>
     * <p>
     * Recall that the <i>homogeneous</i> transfer matrix <b>&Phi;</b> for the ring 
     * has final row that represents the translation <b>&Delta;</b> of the particle
     * for the circuit around the ring.  The 6&times;6 sub-matrix of <b>&Phi;</b> represents
     * the (linear) action of the bending magnetics and quadrupoles and corresponds to the
     * matrix <b>T</b> &in; <b>R</b><sup>6&times;</sup> (here <b>T</b> is linear). 
     * Thus, we can write the linear operator <b>&Phi;</b>
     * as the augmented system 
     * <br/>
     * <br/>
     * <pre>
     * &nbsp; &nbsp; <b>&Phi;</b> = |<b>T</b> <b>&Delta;</b> |,   <b>z</b> &equiv; |<b>p</b>| ,
     *         |<b>0</b> 1 |        |1|
     * </pre> 
     * where <b>p</b> is the projection of <b>z</b> onto the ambient phase space
     * <b>R</b><sup>6</sup> (without homogeneous the homogeneous coordinate).
     * coordinates). 
     * </p>
     * <p>
     * Putting this together we get
     * <br/>
     * <br/>
     * &nbsp; &nbsp; <b>&Phi;z</b> = <b>Tp</b> + <b>&Delta;</b> = <b>p</b> , 
     * <br/>
     * <br/>
     * to which the solution is
     * <br/>
     * <br/>
     * &nbsp; &nbsp; <b>p</b> = -(<b>T</b> - <b>I</b>)<sup>-1</sup><b>&Delta;</b>
     * <br/>
     * <br/>
     * assuming it exists.  The question of solution existence falls upon the
     * resolvent <b>R</b> &equiv; (<b>T</b> - <b>I</b>)<sup>-1</sup> of <b>T</b>.
     * By inspection we can see that <b>p</b> is defined so long as the eigenvalues
     * of <b>T</b> are located away from 1.
     * In this case the returned value is the augmented vector 
     * (<b>p</b> 1)<sup><i>T</i></sup>.
     * </p>
     * <p>
     * When the eigenvectors do contain 1, we attempt to find the solution for the
     * transverse phase space.  That is, we take vector <b>p</b> &in; <b>R</b><sup>4</sup>
     * and <b>T</b> &in; <b>R</b><sup>4&times;4</sup> where 
     * <b>T</b> = proj<sub>4&times;4</sub> <b>&Phi;</b>.  The returned value is then
     * <b>z</b> = (<b>p</b> 0 0 1)<sup><i>T</i></sup>.
     * 
     * @param   matPhi      the one-turn transfer matrix
     * 
     * @return              fixed point solution for the given phase matrix
     *  
     * @author Thomas Pelaia                
     * @author Christopher K. Allen
     * @since  Aug 14, 2013
     */
    public PhaseVector calculateFixedPoint(PhaseMatrix matPhi) {

        R6x6    matT = matPhi.projectR6x6();
        R6x6    matI = R6x6.newIdentity();
        R6x6    matR = matT.minus( matI );

        R6      vecd = matPhi.projectColumn(IND.HOM);
        vecd.negate();

        if (matR.det() > DBL_EPS) {

            R6          vecp = matR.solve(vecd);
            PhaseVector vecP = PhaseVector.embed(vecp);

            return vecP;
        }


        // The system is indeterminant in full 6D phase space
        //   So compute the solution for the transverse phase plane
        R4x4     matTt = matPhi.projectR4x4();
        R4x4     matIt = R4x4.newIdentity();
        R4x4     matRt = matTt.minus( matIt );

        R4       vecdt = new R4();
        vecd.projectOnto(vecdt);

        // Solve system and return it
        R4       vecpt = matRt.solve(vecdt);

        PhaseVector     vecP = PhaseVector.newZero();
        vecP.embedIn(vecpt);

        return  vecP;
    }


    /*
     * Beam Operations
     */

    /**
     * <p>
     * Taken from <code>TransferMapState</code>.
     * </p>
     * <p>
     * Compute and return the betatron phase advance for a particle produced
     * by the given matrix when used as a transfer matrix.
     * </p>
     * 
     * @param	matPhase	the transfer matrix that propagated the Twiss parameters
     * @param   twsOld    Twiss parameter before application of matrix
     * @param   twsNew    Twiss parameter after application of matrix
     * 
     * @return  vector (sigx,sigy,sigz) of phase advances in <b>radians</b>
     * 
     * @deprecated This is superceded by calculatePhaseAdvance which does more extensive checking
     */
    @Deprecated
    public R3   compPhaseAdvance(PhaseMatrix matPhase, Twiss[] twsOld, Twiss[] twsNew)  {

        double  dblPhsAd;   // phase advance
        double  betaOld;    // the beta Twiss parameter (beam size) before propagation
        double  betaNew;    // the beta Twiss parameter (beam size) after propagation

        // Compute the phase advances for each phase plane
        R3      vecPhsAd = new R3();    // returned set of phase advances

        for (int i=0; i<3; i++) {           // Loop through each plane
            int iElem = 2*i;                // phase matrix index

            double dblXXp = matPhase.getElem(iElem, iElem+1);	// <xx'>, <yy'>, <zz'>

            betaOld = twsOld[i].getBeta();
            betaNew = twsNew[i].getBeta();
            dblPhsAd = Math.asin(dblXXp/Math.sqrt(betaOld * betaNew) );

            vecPhsAd.set(i, dblPhsAd);
        }

        return vecPhsAd;
    }

    /**
     * <p>
     * Taken from <code>TransferMapTrajectory</code>.
     * </p>
     * <p>
     * Calculates the phase advance for the given transfer map assuming
     * periodicity, that is, the given matrix represents the transfer matrix <b>&Phi;</b>
     * of at least one cell in a periodic structure.  The Courant-Snyder parameters 
     * of the machine (beam) must be invariant under the action of the transfer matrix 
     * (this indicates a periodic focusing structure 
     * where the beam envelope is modulated by that structure).  One phase advance 
     * is provided for each phase plane, i.e., 
     * (&sigma;<sub><i>x</i></sub>, &sigma;<sub><i>z</i></sub>, &sigma;<sub><i>z</i></sub>).  
     * In the case the given map
     * is the transfer map for one cell in a periodic lattice, in that case the returned 
     * value is the phase advance per cell,
     * &sigma;<sub><i>cell</i></sub> of the periodic system.
     * </p>
     * <p>
     * When the given map is the full turn map of a ring then the phase advances are the 
     * betatron phases for the ring.  Specifically, the sinusoidal phase that the particles
     * advance after each completion of a ring traversal, modulo 2&pi; 
     * (that is, we only take the fractional part).
     * </p>
     * <p>
     * The basic computation is 
     * <br/>
     * <br/>
     * &nbsp; &nbsp;  &sigma; = cos<sup>-1</sup>[&frac12; Tr <b>&Phi;</b>] ,
     * <br/>
     * <br/>
     * where <b>&Phi;</b> is the the provided transfer matrix.
     * </p>
     * 
     * @param   matPhiCell  transfer matrix for a cell in a periodic structure
     * 
     * @return  the array of phase advances (&sigma;<sub><i>x</i></sub>, &sigma;<sub><i>y</i></sub>, &sigma;<sub><i>z</i></sub>)
     * 
     * @author Thomas Pelaia                
     * @author Christopher K. Allen
     * @since  Aug 14, 2013
     */
    private double[] calculatePhaseAdvPerCell(PhaseMatrix matPhiCell) {
        double[]    arrTunes = new double[NUM_MODES];
    
        // H. Sako - Look at the sign of M12, and determine the phase
        // since M12=beta*sin(mu) and beta>0, if M12>0, sin(mu)>0, 0<mu<pi
        //                                    if M12<0, sin(mu)<0, -pi<mu<0
        for ( int imode = 0 ; imode < NUM_MODES ; imode++ ) {
            final int index = 2 * imode;
            double trace = matPhiCell.getElem( index, index ) + matPhiCell.getElem( index + 1, index + 1 );
    
            double m12   = matPhiCell.getElem( index, index+1 );                               
            double sigma = Math.acos( trace / 2 );
            
            // problem is when abs(trace)>1, then _tunes are Double.NaN
            if (m12<0) 
                sigma = -sigma;
            
            arrTunes[imode] = sigma;            
        }
    
        return arrTunes;
    }

    /**
     * <p>
     * Taken from <code>TransferMapTrajectory</code>.
     * </p>
     * <p>
     * Calculates the fractional tunes for the given transfer map assuming
     * periodicity.  That is, the Courant-Snyder parameters of the machine (beam) must be invariant
     * under the action of the transfer matrix (this indicates a periodic focusing structure 
     * where the beam envelope is modulated by that structure).  One tune is provided for 
     * each phase plane, i.e., 
     * (&nu;<sub><i>x</i></sub>, &nu;<sub><i>z</i></sub>, &nu;<sub><i>z</i></sub>).  The given map
     * must in the least be the transfer map for one cell in a periodic 
     * lattice.  In that case the returned value is the fractional phase advance per cell,
     * &sigma;<sub><i>cell</i></sub>/2&pi; of the periodic system.
     * </p>
     * <p>
     * When the given map is the full turn map of a ring then the tunes are the 
     * betatron tunes for the ring.  Specifically, the fraction of a cycle that the particles
     * advance after each completion of a ring traversal, modulo 1 (that is, we only take the
     * fractional part).
     * </p>
     * <p>
     * The basic computation is 
     * <br/>
     * <br/>
     * &nbsp; &nbsp;  &nu; = (1/2&pi;) cos<sup>-1</sup>[&frac12; Tr <b>&Phi;</b>] ,
     * <br/>
     * <br/>
     * where <b>&Phi;</b> is the 2&times;2 diagonal block of the transfer matrix 
     * corresponding to the phase plane of interest.
     * </p>
     * <p>
     * The calculations are actually done by the method 
     * <code>{@link #calculatePhaseAdvPerCell(PhaseMatrix)}</code> then normalized by
     * 2&pi;.
     * </p>
     * 
     * @param   matPhiCell  transfer matrix for a cell in a periodic structure
     * 
     * @return      the array of betatron tunes (&nu;<sub><i>x</i></sub>, &nu;<sub><i>y</i></sub>, &nu;<sub><i>z</i></sub>)
     * 
     * @author Thomas Pelaia                
     * @author Christopher K. Allen
     * @since  Aug 14, 2013
     */
    private double[] calculateTunePerCell(PhaseMatrix matPhiCell) {
        double[]    arrTunes = this.calculatePhaseAdvPerCell(matPhiCell);
        
        for (int i=0; i<arrTunes.length; i++) {
            double  dblTune = arrTunes[i]/DBL_2PI % 1.0;
            
            arrTunes[i] = dblTune;
        }
        
        return arrTunes;
    }

    /** 
     * <p>
     * Taken from <code>TransferMapState</code>.
     * </p>
     * <p>
     * Compute and return the particle phase advance for under the action
     * of the given phase matrix when used as a transfer matrix.
     * </p>
     * <p>
     * Calculates the phase advances given the initial and final 
     * Courant-Snyder &alpha; and &beta; values provided. This is the general
     * phase advance of the particle through the transfer matrix <b>&Phi;</b>, and no special
     * requirements are placed upon <b>&Phi;</b>.   One phase
     * advance is provided for each phase plane, i.e., 
     * (&sigma;<sub><i>x</i></sub>, &sigma;<sub><i>z</i></sub>, &sigma;<sub><i>z</i></sub>).  
     * </p>
     * <p>
     * The definition of phase advance &sigma; is given by
     * <br/>
     * <br/>
     * &nbsp; &nbsp; &sigma;(<i>s</i>) &equiv; &int;<sup><i>s</i></sup> [1/&beta;(<i>t</i>)]<i>dt</i> ,
     * <br/>
     * <br/>
     * where &beta;(<i>s</i>) is the Courant-Snyder, envelope function, and the integral 
     * is taken along the interval between the initial and final Courant-Snyder 
     * parameters.
     * </p>
     * <p>
     * The basic relation used to compute &sigma; is the following:
     * <br/>
     * <br/>
     * &nbsp; &nbsp;  &sigma; = sin<sup>-1</sup> &phi;<sub>12</sub>/(&beta;<sub>1</sub>&beta;<sub>2</sub>)<sup>&frac12;</sup> ,
     * <br/>
     * <br/>
     * where &phi;<sub>12</sub> is the element of <b>&Phi;</b> in the upper right corner of each 
     * 2&times;2 diagonal block, &beta;<sub>1</sub> is the initial beta function value (provided)
     * and &beta;<sub>2</sub> is the final beta function value (provided).
     * </p>
     * 
     * @param   matPhi    the transfer matrix that propagated the Twiss parameters
     * @param   twsOld    Twiss parameter before application of matrix
     * @param   twsNew    Twiss parameter after application of matrix
     * 
     * @return      the array of betatron tunes (&sigma;<sub><i>x</i></sub>, &sigma;<sub><i>z</i></sub>, &sigma;<sub><i>z</i></sub>)
     * 
     * @author  Christopher K. Allen
     * @author  Thomas Pelaia
     * @since   Jun, 2004
     * @version Oct, 2013
     */
    private R3 calculatePhaseAdvance(PhaseMatrix matPhi, Twiss[] twsInit, Twiss[] twsFinal) {

        final Twiss[] twsFnl = twsFinal;
        final Twiss[] twsInt = twsInit;

        final double[] phases = new double[3];
        for ( int mode = 0 ; mode < 3; mode++ ) {
            final double beta = twsFnl[mode].getBeta();
            final double initialBeta = twsInt[mode].getBeta();
            final double initialAlpha = twsInt[mode].getAlpha();
            final double m11 = matPhi.getElem( 2*mode, 2*mode );
            final double m12 = matPhi.getElem( 2*mode, 2*mode + 1 );

            double sinPhase = m12 / Math.sqrt( beta * initialBeta );
            sinPhase = Math.max( Math.min( sinPhase, 1.0 ), -1.0 );     // make sure it is in the range [-1, 1]

            // Sako (I think the following is wrong)          
            //    final double cosPhase = m11 * Math.sqrt( beta / initialBeta ) - initialAlpha * sinPhase;
            //  Replaced by
            final double cosPhase = m11 * Math.sqrt( initialBeta / beta) - initialAlpha * sinPhase;
            //org
            //      final double cosPhase = m11 * Math.sqrt( beta / initialBeta ) - initialAlpha * sinPhase;

            final double   phase  = Math.asin( sinPhase );

            if ( cosPhase >= 0 ) {
                if ( sinPhase >= 0 ) {
                    phases[mode] = phase;

                } else {

                    phases[mode] = 2 * Math.PI + phase;                 
                }

            } else {

                phases[mode] = Math.PI - phase;
            }           
        }

        R3 vecPhases = new R3( phases );

        return vecPhases;
    }

    /** 
     * <p>
     * Taken from <code>TransferMapState</code>.
     * </p>
     * <p>
     * Calculates the matched Courant-Snyder parameters for the given
     * period cell transfer matrix and phase advances.  
     * </p>
     * <p>
     * The given matrix <b>&Phi;</b> is assumed to be the transfer matrix through
     * at least one cell in a periodic lattice.  The array of phase advances 
     * {&sigma;<sub><i>x</i></sub>, &sigma;<sub><i>y</i></sub>, &sigma;<sub><i>x</i></sub>}
     * are assumed to be the particle phase advances through the cell for the matched 
     * solution.   
     * </p> 
     * <p>
     * The returned Courant-Snyder parameters (&alpha;, &beta;, &epsilon;) are invariant
     * under the action of the given phase matrix, that is, they are matched.  All that 
     * is require are &alpha; and &beta; since &epsilon; specifies the size of the beam
     * envelope.  Consequently the returned &epsilon; is <code>NaN</code>.
     * </p>
     * The following are the calculations used for the Courant-Snyder parameters of a 
     * single phase plane:
     * <br/>
     * <br/>
     * &nbsp; &nbsp; &alpha; &equiv; -<i>ww'</i> = (&phi;<sub>11</sub> - &phi;<sub>22</sub>)/(2 sin &sigma;) ,
     * <br/>
     * <br/>
     * &nbsp; &nbsp; &beta; &equiv; <i>w</i><sup>2</sup> = &phi;<sub>12</sub>/sin &sigma;
     * <br/>
     * <br/>
     * where &phi;<sub><i>ij</i></sub> are the elements of the 2&times;2 diagonal blocks of
     * <b>&Phi;</b> corresponding the the particular phase plane, the function <i>w</i>
     * is taken from Reiser, and &sigma; is the phase advance through the cell for the 
     * particular phase plance.
     * </p>
     * 
     * @param   matPhiCell  transfer matrix for a periodic cell
     * @param   arrPhsCell  the phase advances through the periodic cell
     * 
     * @return  array of matched Courant-Snyder parameters 
     *          (&alpha;<sub><i>cell</i></sub>, &beta;<sub><i>cell</i></sub>, <code>NaN</code>) 
     *          for each phase plane
     *          
     *          
     * @author Christopher K. Allen
     * @author Thomas Pelaia
     * @since  Jun 2004
     * @version Oct, 2013
     */
    private Twiss[] calculateMatchedTwiss(PhaseMatrix matPhiCell, double[] arrPhsCell) {

        final Twiss[] arrTwsCell = new Twiss[NUM_MODES];
        for ( int mode = 0 ; mode < NUM_MODES ; mode++ ) {
            final int index = 2 * mode;
            final double sinMu = Math.sin( DBL_2PI * arrPhsCell[mode] );// _tunes could be NaN
            final double m11 = matPhiCell.getElem( index, index );
            final double m12 = matPhiCell.getElem( index, index + 1 );
            final double m22 = matPhiCell.getElem( index + 1, index + 1 );
            final double beta = m12 / sinMu;
            final double alpha = ( m11 - m22 ) / ( 2 * sinMu );
            final double emittance = Double.NaN;
            arrTwsCell[mode] = new Twiss( alpha, beta, emittance );
        }

        return arrTwsCell;
    }

    /** 
     * <p>
     * Taken from <code>TransferMapState</code>.
     * </p>
     * <p>
     * Calculates the fixed point (closed orbit) for the given transfer matrix (assumed to be
     * a one-turn map) in the presence of dispersion.  The fixed point is with regard
     * to the transverse phase plane coordinates.
     * </p>
     * <p>
     * The transverse plane dispersion vector <b>&Delta;</b> is defined  
     * <br/>
     * <br/> 
     * &nbsp; &nbsp; <b>&Delta;</b><sub><i>t</i></sub> &equiv; -(1/&gamma;<sup>2</sup>)[d<i>x</i>/d<i>z'</i>, d<i>x'</i>/d<i>z'</i>, d<i>y</i>/d<i>z'</i>, d<i>y'</i>/d<i>z'</i>]<sup><i>T</i></sup> .
     * <br/>
     * <br/>  
     * It can be identified as the first 4 entries of the 6<sup><i>th</i></sup> 
     * column in the given transfer matrix. The above vector
     * quantifies the change in the transverse particle phase 
     * coordinate position versus the change in particle momentum.  
     * The factor -(1/&gamma;<sup>2</sup>) is needed to convert from longitudinal divergence
     * angle <i>z'</i> used by XAL to momentum &delta;<i>p</i> &equiv; &Delta;<i>p</i>/<i>p</i> used in 
     * the dispersion definition.  Specifically,
     * <br/>
     * <br/>
     * &nbsp; &nbsp; &delta;<i>p</i> &equiv; &Delta;<i>p</i>/<i>p</i> = &gamma;<sup>2</sup><i>z</i>'
     * <br/>
     * <br/>
     * As such, the above vector can be better described
     * <br/>
     * <br/> 
     * &nbsp; &nbsp; <b>&Delta;</b><sub><i>t</i></sub> &equiv; [&Delta;<i>x</i>/&delta;<i>p</i>, &Delta;<i>x'</i>/&delta;<i>p</i>, &Delta;<i>y</i>/&delta;<i>p</i>, &Delta;<i>y'</i>/&delta;<i>p</i>]<sup><i>T</i></sup>
     * <br/>
     * <br/>
     * explicitly describing the change in transverse phase coordinate for fractional
     * change in momentum &delta;<i>p</i>.  
     * </p>
     * <p>
     * Since we are only concerned with transverse phase space coordinates, we restrict ourselves to the 
     * 4&times;4 upper diagonal block of <b>&Phi;</b>, which we denote take <b>T</b>.  
     * That is, <b>T</b> = &pi; &sdot; <b>&Phi;</b>
     * where &pi; : <b>R</b><sup>6&times;6</sup> &rarr; <b>R</b><sup>4&times;4</sup> is the
     * projection operator. 
     * </p>
     * <p>
     * This method finds that point <b>z</b><sub><i>t</i></sub> &equiv; 
     * (<i>x<sub>t</sub></i>, <i>x'<sub>t</sub></i>, <i>y<sub>t</sub></i>, <i>y'<sub>t</sub></i>)
     * in transvse phase space that is invariant under the action of the ring for a given momentum spread
     * &delta;<i>p</i>.  That is, the particle ends up
     * in the same location each revolution. With a finite momentum spread of &delta;<i>p</i> &gt; 0
     * we require this require that
     * <br/>
     * <br/>
     * &nbsp; &nbsp; <b>Tz</b><sub><i>t</i></sub> + &delta;<i>p</i><b>&Delta;</b><sub><i>t</i></sub> = <b>z</b><sub><i>t</i></sub> ,
     * <br/>
     * <br/>
     * which can be written
     * <br/>
     * <br/>
     * &nbsp; <b>z</b><sub><i>t</i></sub> = &delta;<i>p</i>(<b>T</b> - <b>I</b>)<sup>-1</sup><b>&Delta;</b><sub><i>t</i></sub> ,
     * <br/>
     * <br/>
     * where <b>I</b> is the identity matrix.  Dividing both sides by &delta;<i>p</i> yields the final
     * result
     * <br/>
     * <br/>
     * &nbsp; <b>z</b><sub>0</sub> &equiv; <b>z</b><sub><i>t</i></sub>/&delta;<i>p</i> = (<b>T</b> - <b>I</b>)<sup>-1</sup><b>&Delta;</b><sub><i>t</i></sub> ,
     * <br/>
     * <br/>
     * which is the returned value of this method.  It is normalized by
     * &delta;<i>p</i> so that we can compute the closed orbit for any given momentum spread.
     * </p>
     *   
     * @param matPhi	we are calculating the dispersion of a ring with this one-turn map
     * @param gamma		relativistic factor
     * 
     * @return         The closed orbit fixed point <b>z</b><sub>0</sub> for finite 
     *                 dispersion, normalized by momentum spread.
     *                 Returned as an array [<i>x</i><sub>0</sub>,<i>x'</i><sub>0</sub>,<i>y</i><sub>0</sub>,<i>y'</i><sub>0</sub>]/&delta;<i>p</i>
     *
     * @author Thomas Pelaia                
     * @author Christopher K. Allen
     * @since  Aug 14, 2013
     */
    public double[] calculateDispersion(final PhaseMatrix matPhi, final double gamma) {

        // Decompose the transfer matrix into transverse and longitudinal 
        //    components.  The decomposition contains the linear algebraic
        //    system we need to solve the dispersion relations.

        // Extract the transverse divergence-angle vector
        R6    vecdP = matPhi.projectColumn(IND.Zp);
        R4    vecdp = new R4();
        vecdP.projectOnto(vecdp);

        // Convert it to momentum
        vecdp.timesEquals(- 1.0/(gamma*gamma) );


        // Extract the transverse phase space transfer matrix
        //    then set up the fixed point system.
        R4x4  matT = matPhi.projectR4x4();
        R4x4  matI = R4x4.newIdentity();
        R4x4  matR = matT.minus(matI);


        // Solve for the dispersion vector and return it
        R4    vecd = matR.solve(vecdp);

        double[]  arrd = vecd.getArrayCopy();

        return arrd;
    }

    //	    public Twiss[] getTwiss() {
    //	            final double PI2 = 2 * Math.PI;
    //	            final double[] tunes = _trajectory.getTunes();
    //	            final PhaseMatrix matrix = _phaseMatrix;
    //	            
    //	            final Twiss[] twiss = new Twiss[NUM_MODES];
    //	            for ( int mode = 0 ; mode < NUM_MODES ; mode++ ) {
    //	                final int index = 2 * mode;
    //	                final double sinMu = Math.sin( PI2 * tunes[mode] );// _tunes could be NaN
    //	                final double m11 = matrix.getElem( index, index );
    //	                final double m12 = matrix.getElem( index, index + 1 );
    //	                final double m22 = matrix.getElem( index + 1, index + 1 );
    //	                final double beta = m12 / sinMu;
    //	                final double alpha = ( m11 - m22 ) / ( 2 * sinMu );
    //	                final double emittance = Double.NaN;
    //	                twiss[mode] = new Twiss( alpha, beta, emittance );
    //	            }
    //	            
    //	            return twiss;
    //	    }




    //	/** 
    //	 * Calculate the fixed point solution vector representing the closed orbit at the location of this element.
    //	 * We find the fixed point for the six phase space coordinates.
    //	 * The equation to solve is <code>Ax + b = 0</code> where <code>A</code> is the 6x6 submatrix less the identity
    //	 * matrix and <code>b</code> is the 7th column excluding the 7th row element.  The reason for this is that the
    //	 * fixed point is defined by the point for which the transfer map maps to the same point.  This is
    //	 * <code>M * v = v</code>.  
    //	 * 
    //	 * @return the fixed point solution
    //	 */
    //	public PhaseVector calculateFixedPoint(PhaseMatrix matPhi) {
    //		Matrix A = _phaseMatrix.getMatrix().getMatrix( 0, PhaseMatrix.IND_ZP, 0, PhaseMatrix.IND_ZP ).minus( Matrix.identity(PhaseMatrix.IND_ZP+1, PhaseMatrix.IND_ZP+1) );
    //		Matrix b = _phaseMatrix.getMatrix().getMatrix( 0, PhaseMatrix.IND_ZP, PhaseMatrix.IND_HOM, PhaseMatrix.IND_HOM ).times( -1 );
    //
    //		//sako
    //		//Matrix MZ = m_matPhase.getMatrix(IND_Z,IND_ZP,IND_Z,IND_ZP);
    //		//      System.out.println("det(MZ), det(A) = "+MZ.det()+" "+A.det());
    //		//      System.out.println("###### MZ = ("+MZ.get(0,0)+","+MZ.get(0,1)+")("+MZ.get(1,0)+","+MZ.get(1,1)+")");
    //
    //		PhaseVector sol;
    //
    //		if (A.det()==0) {
    //			Matrix Axy = _phaseMatrix.getMatrix().getMatrix( 0, PhaseMatrix.IND_YP, 0, PhaseMatrix.IND_YP ).minus( Matrix.identity(PhaseMatrix.IND_YP+1, PhaseMatrix.IND_YP+1) );
    //			Matrix bxy = _phaseMatrix.getMatrix().getMatrix( 0, PhaseMatrix.IND_YP, PhaseMatrix.IND_HOM, PhaseMatrix.IND_HOM ).times( -1 );
    //			Matrix solutionxy = Axy.solve(bxy);
    //			//System.out.println("A.det()=0, sxy solved");
    //			sol = new PhaseVector( solutionxy.get(PhaseMatrix.IND_X, 0), solutionxy.get(PhaseMatrix.IND_XP, 0), solutionxy.get(PhaseMatrix.IND_Y, 0), solutionxy.get(PhaseMatrix.IND_YP, 0), 0, 0 );//sako, check z, zp components!
    //		} else {
    //
    //			Matrix solution = A.solve(b);
    //			sol = new PhaseVector( solution.get(PhaseMatrix.IND_X, 0), solution.get(PhaseMatrix.IND_XP, 0), solution.get(PhaseMatrix.IND_Y, 0), solution.get(PhaseMatrix.IND_YP, 0), solution.get(PhaseMatrix.IND_Z, 0), solution.get(PhaseMatrix.IND_ZP, 0) );
    //		}
    //		return sol;
    //	}

    //	    public Twiss[] getTwiss() {
    //	            final double PI2 = 2 * Math.PI;
    //	            final double[] tunes = _trajectory.getTunes();
    //	            final PhaseMatrix matrix = _phaseMatrix;
    //	            
    //	            final Twiss[] twiss = new Twiss[NUM_MODES];
    //	            for ( int mode = 0 ; mode < NUM_MODES ; mode++ ) {
    //	                final int index = 2 * mode;
    //	                final double sinMu = Math.sin( PI2 * tunes[mode] );// _tunes could be NaN
    //	                final double m11 = matrix.getElem( index, index );
    //	                final double m12 = matrix.getElem( index, index + 1 );
    //	                final double m22 = matrix.getElem( index + 1, index + 1 );
    //	                final double beta = m12 / sinMu;
    //	                final double alpha = ( m11 - m22 ) / ( 2 * sinMu );
    //	                final double emittance = Double.NaN;
    //	                twiss[mode] = new Twiss( alpha, beta, emittance );
    //	            }
    //	            
    //	            return twiss;
    //	    }




    /*
     * 
     * Unused, courtesy of ICoordinateState & IProbeState
     * 
     * 
     */

    /*
     * IPhaseState Interface 
     */

    /**
     *
     * @see xal.model.probe.traj.IPhaseState#getTwiss()
     *
     * @author Christopher K. Allen
     * @since  Aug 14, 2013
     */
    @Override
    public Twiss[] getTwiss() {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     *
     * @see xal.model.probe.traj.IPhaseState#getBetatronPhase()
     *
     * @author Christopher K. Allen
     * @since  Aug 14, 2013
     */
    @Override
    public R3 getBetatronPhase() {
        // TODO Auto-generated method stub
        return null;
    }


    /*
     * ICoordinateState Interface
     */

    /**
     *
     * @see xal.model.probe.traj.ICoordinateState#getPhaseCoordinates()
     *
     * @author Christopher K. Allen
     * @since  Oct 22, 2013
     */
    @Override
    public PhaseVector getPhaseCoordinates() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhaseVector getFixedOrbit() { return null; }




    /*
     * IProbeState Interface
     */

    @Override
    public void setKineticEnergy(double W) { }

    @Override
    public double getKineticEnergy() { return Double.NaN; }

    @Override
    public void setTime(double t) { }

    @Override
    public double getTime() { return Double.NaN; }

    @Override
    public void setPosition(double s) { }

    @Override
    public double getPosition() { return Double.NaN; }

    @Override
    public void setSpeciesCharge(double q) { }

    @Override
    public double getSpeciesCharge() {return Double.NaN; }

    @Override
    public void setSpeciesRestEnergy(double Er) { }

    @Override
    public double getSpeciesRestEnergy() { return Double.NaN; }

    @Override
    public void setElementId(String id) { }

    @Override
    public String getElementId() { return null; }

    @Override
    public void load(DataAdaptor da) { }

    @Override
    public void save(DataAdaptor da) { }



    /*
     * Support Methods
     */
}

