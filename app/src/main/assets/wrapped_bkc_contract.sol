// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/**
 * @title wBKC
 * @notice Minimal wrapped BKC contract for BrokerChain.
 * @dev Native BKC can be wrapped 1:1 into ERC-20 style balances and unwrapped back to native BKC.
 */
contract wBKC {
    string public name = "wrapped BKC";
    string public symbol = "wBKC";
    uint8 public decimals = 18;

    event Approval(address indexed owner, address indexed spender, uint256 value);
    event Transfer(address indexed from, address indexed to, uint256 value);
    event Deposit(address indexed account, uint256 value);
    event Withdrawal(address indexed account, uint256 value);

    mapping(address => uint256) public balanceOf;
    mapping(address => mapping(address => uint256)) public allowance;

    receive() external payable {
        deposit();
    }

    function deposit() public payable {
        balanceOf[msg.sender] += msg.value;
        emit Deposit(msg.sender, msg.value);
    }

    function withdraw(uint256 value) public {
        require(balanceOf[msg.sender] >= value, "wBKC: insufficient balance");

        balanceOf[msg.sender] -= value;

        (bool ok, ) = payable(msg.sender).call{value: value}("");
        require(ok, "wBKC: native transfer failed");

        emit Withdrawal(msg.sender, value);
    }

    function totalSupply() public view returns (uint256) {
        return address(this).balance;
    }

    function approve(address spender, uint256 value) public returns (bool) {
        allowance[msg.sender][spender] = value;
        emit Approval(msg.sender, spender, value);
        return true;
    }

    function transfer(address to, uint256 value) public returns (bool) {
        return transferFrom(msg.sender, to, value);
    }

    function transferFrom(address from, address to, uint256 value) public returns (bool) {
        require(balanceOf[from] >= value, "wBKC: insufficient balance");

        if (from != msg.sender && allowance[from][msg.sender] != type(uint256).max) {
            require(allowance[from][msg.sender] >= value, "wBKC: insufficient allowance");
            allowance[from][msg.sender] -= value;
        }

        balanceOf[from] -= value;
        balanceOf[to] += value;
        emit Transfer(from, to, value);
        return true;
    }
}
