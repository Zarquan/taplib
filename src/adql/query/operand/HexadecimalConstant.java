package adql.query.operand;

/*
 * This file is part of ADQLLibrary.
 *
 * ADQLLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ADQLLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ADQLLibrary.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2018, ROE (http://www.roe.ac.uk/)
 *
 */
import adql.query.ADQLObject;

/**
 * A hexadecimal constant.
 *
 * @author Dave Morris (ROE)
 * 11/2018
 */
public class HexadecimalConstant extends NumericConstant implements ADQLOperand {

    /**
     * The prefix for hexadecimal literals.
     *
     */
    public static final String PREFIX = "0x";

    /**
     * Create a hexadecimal constant from a long.
     *
     */
    public HexadecimalConstant(final long value){
        super(value);
    }

    /**
     * Create a hexadecimal constant from a String.
     *
     */
    public HexadecimalConstant(final String value, final boolean check) throws NumberFormatException{
        super(value, check);
    }

    /**
     * Copy a hexadecimal constant.
     *
     */
    public HexadecimalConstant(final HexadecimalConstant origin){
        super(origin.getValue(), false);
    }

    @Override
    public final void setValue(final double value){
        this.setValue((long) value);
    }

    @Override
    public final void setValue(final long value){
        this.value = PREFIX + Long.toHexString(value);
    }

    @Override
    public void setValue(final String value, final boolean check) throws NumberFormatException {
        if (check){
            this.getLongValue(value);
        }
        this.value = value;
    }

    protected double getDoubleValue() throws NumberFormatException{
        return this.getDoubleValue(this.value);
    }

    protected double getDoubleValue(final String value) throws NumberFormatException{
        return new Double(getLongValue(value));
    }

    protected long getLongValue(final String value) throws NumberFormatException{
        if (value.startsWith(PREFIX)){
            return Long.parseLong(value.substring(2),16);
        }
        else{
            return Long.parseLong(value,16);
        }
    }

    @Override
    public double getNumericValue(){
        try{
            return this.getDoubleValue();
        }catch(final NumberFormatException ouch){
            return Double.NaN;
        }
    }

    @Override
    public ADQLObject getCopy(){
        return new HexadecimalConstant(this);
    }
}

